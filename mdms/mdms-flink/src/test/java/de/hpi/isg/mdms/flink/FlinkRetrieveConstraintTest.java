package de.hpi.isg.mdms.flink;


import de.hpi.isg.mdms.domain.RDBMSMetadataStore;
import de.hpi.isg.mdms.domain.constraints.*;
import de.hpi.isg.mdms.flink.serializer.*;
import de.hpi.isg.mdms.model.MetadataStore;
import de.hpi.isg.mdms.model.constraints.ConstraintCollection;
import de.hpi.isg.mdms.model.location.DefaultLocation;
import de.hpi.isg.mdms.model.targets.Column;
import de.hpi.isg.mdms.model.targets.Schema;
import de.hpi.isg.mdms.rdbms.SQLiteInterface;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.LocalCollectionOutputFormat;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class FlinkRetrieveConstraintTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private File testDb;
    private Connection connection;
	private MetadataStore store;
	private Column col1;
	private Column col2;
	private ConstraintCollection dummyConstraintCollection;

    @Before
    public void setUp() {
        this.logger.info("setUp() started.");

        try {
            this.testDb = File.createTempFile("test", ".db");
            this.testDb.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + this.testDb.toURI().getPath());
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        this.store =  RDBMSMetadataStore.createNewInstance(new SQLiteInterface(connection));
        
        final Schema dummySchema1 = store.addSchema("PDB", null, new DefaultLocation());
        col1 = dummySchema1.addTable(store, "table1", null, new DefaultLocation()).addColumn(store,
                "foo", null, 1);
        col2 = dummySchema1.addTable(store, "table1", null, new DefaultLocation()).addColumn(store,
                "bar", null, 2);

       dummyConstraintCollection = store.createConstraintCollection(null, dummySchema1);
        
    }

    @After
    public void tearDown() {
            store.close();
            this.testDb.delete();
    }    
    
    @Test
    public void testDistinctValueCount() throws Exception {
        
        //write dvcs to database
        DistinctValueCount.buildAndAddToCollection(new SingleTargetReference(this.col1.getId()),
                    this.dummyConstraintCollection, 1);

        DistinctValueCount.buildAndAddToCollection(new SingleTargetReference(this.col2.getId()),
                this.dummyConstraintCollection, 3);

        this.store.flush();
        
        //flink job
    	ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
    	DataSet<Tuple> constraints = FlinkMetdataStoreAdapter.getConstraintsFromCollection(env, this.store, this.dummyConstraintCollection, new DVCFlinkSerializer());
        
        List<Tuple2<Integer, Integer>> outData = new ArrayList<Tuple2<Integer, Integer>>();
        constraints.output(new LocalCollectionOutputFormat(outData));

        env.execute("Distinct Value Count Reading");
        
        assertTrue(outData.size() == 2);
    }

    @Test
    public void testDistinctValueOverlap() throws Exception {
    	
    	DistinctValueOverlap.Reference reference = new DistinctValueOverlap.Reference(this.col1.getId(), this.col2.getId());
    	DistinctValueOverlap.buildAndAddToCollection(2, reference, dummyConstraintCollection);
    	this.store.flush();
    	
        //flink job
    	ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
    	DataSet<Tuple> constraints = FlinkMetdataStoreAdapter.getConstraintsFromCollection(env, this.store, this.dummyConstraintCollection, new DVOFlinkSerializer());
        
        List<Tuple3<Integer, Integer, Integer>> outData = new ArrayList<Tuple3<Integer, Integer, Integer>>();
        constraints.output(new LocalCollectionOutputFormat(outData));

        env.execute("Distinct Value Overlap Reading");
        
        assertTrue(outData.size() == 1);
    }

    
    
    @Test
    public void testInclusionDependency() throws Exception {

    	int[] referenced = {col1.getId()};
    	int[] dependent = {col2.getId()};
    	InclusionDependency.Reference reference = new InclusionDependency.Reference(dependent, referenced);
    	InclusionDependency.buildAndAddToCollection(reference, this.dummyConstraintCollection);
    	this.store.flush();
    	
    	 //flink job
    	ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
    	DataSet<Tuple> constraints = FlinkMetdataStoreAdapter.getConstraintsFromCollection(env, this.store, this.dummyConstraintCollection, new INDFlinkSerializer());
        
        List<Tuple2<int[], int[]>> outData = new ArrayList<Tuple2<int[], int[]>>();
        constraints.output(new LocalCollectionOutputFormat(outData));

        env.execute("Inclusion Dependency Reading");
        
        assertTrue(outData.size() == 1);    	
    }
    
    @Test
    public void testUniqueColumnCombination() throws Exception {

    	int[] columns = {col1.getId()};
    	UniqueColumnCombination.buildAndAddToCollection(new UniqueColumnCombination.Reference(columns), this.dummyConstraintCollection);
    	this.store.flush();
    	
   	 //flink job
    	ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
    	DataSet<Tuple> constraints = FlinkMetdataStoreAdapter.getConstraintsFromCollection(env, this.store, this.dummyConstraintCollection, new UCCFlinkSerializer());
       
       List<Tuple2<int[], int[]>> outData = new ArrayList<Tuple2<int[], int[]>>();
       constraints.output(new LocalCollectionOutputFormat(outData));

       env.execute("Unique Column Combination Reading");
       
       assertTrue(outData.size() == 1);    	
      
    }
    
    @Test
    public void testFunctionalDependency() throws Exception {

    	int[] lhs = {col1.getId()};
    	int rhs = col2.getId();
    	FunctionalDependency.Reference reference = new FunctionalDependency.Reference(rhs, lhs);
    	FunctionalDependency.buildAndAddToCollection(reference, this.dummyConstraintCollection);
    	this.store.flush();
    	
      	 //flink job
       	ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
       	DataSet<Tuple> constraints = FlinkMetdataStoreAdapter.getConstraintsFromCollection(env, this.store, this.dummyConstraintCollection, new FDFlinkSerializer());
          
          List<Tuple2<int[], int[]>> outData = new ArrayList<Tuple2<int[], int[]>>();
          constraints.output(new LocalCollectionOutputFormat(outData));

          env.execute("Functional Dependency Reading");
          
          assertTrue(outData.size() == 1);    	
    	
    }


}
