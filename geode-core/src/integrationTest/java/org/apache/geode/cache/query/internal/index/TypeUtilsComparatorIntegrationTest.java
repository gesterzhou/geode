package org.apache.geode.cache.query.internal.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.DataSerializable;
import org.apache.geode.DataSerializer;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.data.Portfolio;
import org.apache.geode.test.junit.rules.ServerStarterRule;

public class TypeUtilsComparatorIntegrationTest {
  Properties properties = new Properties() {

  };

  @Rule
  public ServerStarterRule
      serverStarterRule =
      new ServerStarterRule().withProperty("enable-time-statistics", "true")
          .withProperty("statistic-sampling-enabled", "true")
          .withProperty("statistic-sample-rate", "1000")
          .withProperty("statistic-archive-file", "TypeUtils.gfs").withAutoStart();

  private Cache cache;
  QueryService queryService;

  @Before
  public void setup() {
    String regionName = "portfolio";

    cache = serverStarterRule.getCache();
    assertNotNull(cache);
    Region region =
        cache.createRegionFactory().setDataPolicy(DataPolicy.REPLICATE).create(regionName);

//    for (int i = 0; i < 100; i++) {
//      Portfolio p = new Portfolio(i);
//      region.put(i, p);
//    }

    for (int i = 0; i < 1000; i++) {
      MyNumericObject p = new MyNumericObject(i);
      region.put(i, p);
    }

    queryService = cache.getQueryService();
  }

//  @Test
//  public void compareQueryShouldNotCreateComparatorEachTime() throws Exception {
//    long then = System.currentTimeMillis();
//    SelectResults results = (SelectResults) queryService
//        .newQuery("select ID from /portfolio p where p.ID = 90f").execute();
//    long now = System.currentTimeMillis();
//    System.out.println("Duration is: "+(now-then));
//
//    assertEquals(1, results.size());
//  }

  @Test
  public void compareQueryShouldNotCreateComparatorEachTime() throws Exception {
    long then = System.currentTimeMillis();
    SelectResults results = (SelectResults) queryService
        .newQuery("select ID from /portfolio p where p.ID = 90f").execute();
    long now = System.currentTimeMillis();
    System.out.println("Duration is: " + (now - then));

    assertEquals(1, results.size());
    Thread.sleep(30000);
  }

  static class MyNumericObject implements Serializable, DataSerializable {
    public int ID;
    public Date createDate;

    MyNumericObject(int id) {
      this.ID = id;
    }

    @Override
    public void toData(DataOutput out) throws IOException {
      out.writeInt(this.ID);
      DataSerializer.writeDate(createDate, out);
    }

    @Override
    public void fromData(DataInput in) throws IOException, ClassNotFoundException {
      this.ID = in.readInt();
      this.createDate = DataSerializer.readDate(in);
    }

    public int hashCode() {
      return this.ID;
    }

    public boolean equals(Object o) {
      if (!(o instanceof MyNumericObject)) {
        return false;
      }
      MyNumericObject p2 = (MyNumericObject) o;
      return this.ID == p2.ID;
    }
  }
}
