package me.prettyprint.cassandra.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.query.SuperCountQuery;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.CounterColumn;
import org.apache.cassandra.thrift.CounterDeletion;
import org.apache.cassandra.thrift.CounterMutation;
import org.apache.cassandra.thrift.CounterSuperColumn;
import org.apache.cassandra.thrift.Deletion;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SuperColumn;
import org.junit.Before;
import org.junit.Test;

public class BatchMutationTest {

  private List<String> columnFamilies;
  private BatchMutation<String> batchMutate;

  @Before
  public void setup() {
    columnFamilies = new ArrayList<String>();
    columnFamilies.add("Standard1");
    batchMutate = new BatchMutation<String>(StringSerializer.get());
  }

  @Test
  public void testAddInsertion() {
    Column column = new Column(StringSerializer.get().toByteBuffer("c_name"), StringSerializer.get().toByteBuffer("c_val"), System.currentTimeMillis());
    batchMutate.addInsertion("key1", columnFamilies, column);
    // assert there is one outter map row with 'key' as the key
    Map<ByteBuffer, Map<String, List<Mutation>>> mutationMap = batchMutate.getMutationMap();

    assertEquals(1, mutationMap.get(StringSerializer.get().toByteBuffer("key1")).size());

    // add again with a different column and verify there is one key and two mutations underneath
    // for "standard1"
    Column column2 = new Column(StringSerializer.get().toByteBuffer("c_name2"), StringSerializer.get().toByteBuffer("c_val2"), System.currentTimeMillis());
    batchMutate.addInsertion("key1",columnFamilies, column2);
    assertEquals(2, mutationMap.get(StringSerializer.get().toByteBuffer("key1")).get("Standard1").size());
  }

  @Test
  public void testAddSuperInsertion() {
    SuperColumn sc = new SuperColumn(StringSerializer.get().toByteBuffer("c_name"),
        Arrays.asList(new Column(StringSerializer.get().toByteBuffer("c_name"), StringSerializer.get().toByteBuffer("c_val"), System.currentTimeMillis())));
    batchMutate.addSuperInsertion("key1", columnFamilies, sc);
    // assert there is one outter map row with 'key' as the key
    assertEquals(1, batchMutate.getMutationMap().get(StringSerializer.get().toByteBuffer("key1")).size());

    // add again with a different column and verify there is one key and two mutations underneath
    // for "standard1"
    SuperColumn sc2 = new SuperColumn(StringSerializer.get().toByteBuffer("c_name2"),
        Arrays.asList(new Column(StringSerializer.get().toByteBuffer("c_name"), StringSerializer.get().toByteBuffer("c_val"), System.currentTimeMillis())));
    batchMutate.addSuperInsertion("key1", columnFamilies, sc2);
    assertEquals(2, batchMutate.getMutationMap().get(StringSerializer.get().toByteBuffer("key1")).get("Standard1").size());
  }


  @Test
  public void testAddDeletion() {
    Deletion deletion = new Deletion(System.currentTimeMillis());
    SlicePredicate slicePredicate = new SlicePredicate();
    slicePredicate.addToColumn_names(StringSerializer.get().toByteBuffer("c_name"));
    deletion.setPredicate(slicePredicate);
    batchMutate.addDeletion("key1", columnFamilies, deletion);

    assertEquals(1,batchMutate.getMutationMap().get(StringSerializer.get().toByteBuffer("key1")).size());

    deletion = new Deletion(System.currentTimeMillis());
    slicePredicate = new SlicePredicate();
    slicePredicate.addToColumn_names(StringSerializer.get().toByteBuffer("c_name2"));
    deletion.setPredicate(slicePredicate);
    batchMutate.addDeletion("key1", columnFamilies, deletion);
    assertEquals(2,batchMutate.getMutationMap().get(StringSerializer.get().toByteBuffer("key1")).get("Standard1").size());
  }
  
  @Test
  public void testIsEmpty() {
    assertTrue(batchMutate.isEmpty());
    
    // Insert a column
    Column c1 = new Column(
        StringSerializer.get().toByteBuffer("c_name"), 
        StringSerializer.get().toByteBuffer("c_val"), 
        System.currentTimeMillis());
    batchMutate.addInsertion("key1", columnFamilies, c1);
    assertFalse(batchMutate.isEmpty());

    // Insert a Counter.
    CounterColumn cc1 = new CounterColumn(StringSerializer.get().toByteBuffer("c_name"), 13);
    batchMutate.addCounterInsertion("key1", columnFamilies, cc1);
    assertFalse(batchMutate.isEmpty());
    
    // Delete the Column/SuperColumn mutations and it should be still not empty
    batchMutate.getMutationMap().clear();
    assertFalse(batchMutate.isEmpty());
    
    // Delete the Counter mutations. Now the batchMutate should be empty.
    batchMutate.getCounterMutationMap().clear();
    assertTrue(batchMutate.isEmpty());
  }
  
  // ********** Test Counters related operations ******************
  
  @Test
  public void testAddCounterInsertion() {

    // Insert a Counter.
    CounterColumn cc1 = new CounterColumn(StringSerializer.get().toByteBuffer("c_name"), 222);
    
    batchMutate.addCounterInsertion("key1", columnFamilies, cc1);
    
    // assert there is one outter map row with 'key' as the key
    Map<ByteBuffer, Map<String, List<CounterMutation>>> mutationMap = batchMutate.getCounterMutationMap();
    assertEquals(1, mutationMap.get(StringSerializer.get().toByteBuffer("key1")).size());

    // add again with a different counter and verify there is one key and two mutations underneath
    // for "standard1"
    CounterColumn cc2 = new CounterColumn(StringSerializer.get().toByteBuffer("c_name2"), 44);
    batchMutate.addCounterInsertion("key1", columnFamilies, cc2);
    assertEquals(2, mutationMap.get(StringSerializer.get().toByteBuffer("key1")).get("Standard1").size());
  }
  
  @Test
  public void testAddCounterDeletion() {
   
    CounterDeletion counterDeletion = new CounterDeletion();
    
    SlicePredicate slicePredicate = new SlicePredicate();
    slicePredicate.addToColumn_names(StringSerializer.get().toByteBuffer("c_name"));
    counterDeletion.setPredicate(slicePredicate);
    
    batchMutate.addCounterDeletion("key1", columnFamilies, counterDeletion);

    assertEquals(1, batchMutate.getCounterMutationMap().get(StringSerializer.get().toByteBuffer("key1")).size());

    counterDeletion = new CounterDeletion();
    slicePredicate = new SlicePredicate();
    slicePredicate.addToColumn_names(StringSerializer.get().toByteBuffer("c_name2"));
    counterDeletion.setPredicate(slicePredicate);
    batchMutate.addCounterDeletion("key1", columnFamilies, counterDeletion);
    assertEquals(2,batchMutate.getCounterMutationMap().get(StringSerializer.get().toByteBuffer("key1")).get("Standard1").size());
  }
  
  @Test
  public void testAddSuperCounterInsertion() {
    // Create 1 super counter.
    CounterSuperColumn csc1 = new CounterSuperColumn(StringSerializer.get().toByteBuffer("c_name"), 
        Arrays.asList(new CounterColumn(StringSerializer.get().toByteBuffer("c_name"), 123)));

    batchMutate.addSuperCounterInsertion("key1", columnFamilies, csc1);
    // assert there is one outter map row with 'key' as the key
    assertEquals(1, batchMutate.getCounterMutationMap().get(StringSerializer.get().toByteBuffer("key1")).size());

    // add again with a different column and verify there is one key and two mutations underneath
    // for "standard1"
    CounterSuperColumn csc2 = new CounterSuperColumn(StringSerializer.get().toByteBuffer("c_name2"), 
            Arrays.asList(new CounterColumn(StringSerializer.get().toByteBuffer("c_name"), 456)));
    batchMutate.addSuperCounterInsertion("key1", columnFamilies, csc2);
    assertEquals(2, batchMutate.getCounterMutationMap().get(StringSerializer.get().toByteBuffer("key1")).get("Standard1").size());
  }
}
