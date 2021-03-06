package com.nice.zoocache
/**
 * Copyright (C) 2012 NICE Systems ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Arnon Rotem-Gal-Oz
 * @version %I%, %G%
 *          <p/>
 */
import org.scalatest.{BeforeAndAfterAll, FunSpec}
import com.netflix.curator.test.TestingServer
import akka.util.duration._



class LocalShadowSpec extends FunSpec with BeforeAndAfterAll{
  val server=new TestingServer(9999)
  val testCluster=server.getConnectString
  val cache  = new SyncZooCache(testCluster,"test",true,maxWait=10 seconds)
  val second = new SyncZooCache(testCluster,"test",true,maxWait=10 seconds)



  it("should fetch from remote cache if local copy is mising"){
    val t=new Test()
    t.name="remote"
    val key="remote"

    second.put(key,t)
    val result=cache.get[Test](key)
    assert(result.get.name===t.name)

  }

  it("should serve local copy if local copy is not expired") {
    val t=new Test()
    t.name="lremote"
    val key="localValid"

    second.put(key,t,ZooCacheSystem.FOREVER)
    val firstResult=cache.get[Test](key)

    t.name="lchanged"
    second.put(key,t,ZooCacheSystem.FOREVER)
    val secondResult=cache.get[Test](key)

    assert(firstResult.get.name===secondResult.get.name)
  }

  it("should fetch from remote cache if local copy expired") {
    val t=new Test()
    t.name="remote"
    val key="expired"

    second.put(key,t,100)
    val firstResult=cache.get[Test](key)
    assert(firstResult.get.name===t.name)

    Thread.sleep(2000)
    t.name="changed"
    second.put(key,t,ZooCacheSystem.FOREVER)
    val secondResult=cache.get[Test](key)
    assert(secondResult.get.name===t.name)

  }

  it("should invalidate local copy if asked") {
    val t=new Test()
    t.name="remote"
    val key="invalidated"

    second.put(key,t,ZooCacheSystem.FOREVER)
    val firstResult=cache.get[Test](key)
    assert(firstResult.get.name===t.name)

    t.name="changed"
    second.put(key,t,ZooCacheSystem.FOREVER)
    second.invalidate()
    Thread.sleep(1000)
    val secondResult=cache.get[Test](key)

    assert(secondResult.get.name==t.name)
  }

  it("should can invalidate more than once") {
    val t=new Test()
    t.name="remote"
    val key="invalidated2"

    second.put(key,t,ZooCacheSystem.FOREVER)
    val firstResult=cache.get[Test](key)
    assert(firstResult.get.name===t.name)

    t.name="changed"
    second.put(key,t,ZooCacheSystem.FOREVER)
    second.invalidate()
    Thread.sleep(1000)
    val secondResult=cache.get[Test](key)

    t.name="changed again"
    second.put(key,t,ZooCacheSystem.FOREVER)
    second.invalidate()
    Thread.sleep(1000)
    val thirdResult=cache.get[Test](key)

    assert(thirdResult.get.name==t.name)
  }

  it("can handle the same key in different caches") (pending) //change all calls to localShadow to concatenate the cacheId to the key

  override def afterAll{
  //  cache.shutdown()
  //  second.shutdown()
  }
}
