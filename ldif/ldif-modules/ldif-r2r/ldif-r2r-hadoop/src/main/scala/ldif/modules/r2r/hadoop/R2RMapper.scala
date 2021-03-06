/* 
 * LDIF
 *
 * Copyright 2011-2014 Universität Mannheim, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ldif.modules.r2r.hadoop

import ldif.hadoop.types.QuadWritable
import org.apache.hadoop.io.{NullWritable, IntWritable}
import ldif.hadoop.utils.HadoopHelper
import org.apache.hadoop.mapred._
import java.io.{FileInputStream, ObjectInputStream}
import ldif.entity.{EntityDescriptionMetadata, EntityWritable}
import de.fuberlin.wiwiss.r2r.LDIFMapping

/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 11/17/11
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */

class R2RMapper extends MapReduceBase with Mapper[IntWritable, EntityWritable, NullWritable, QuadWritable] {
  var mappings: IndexedSeq[LDIFMapping] = null

  override def configure(conf: JobConf) {
    mappings = getMappings(conf)
  }

  /**
   * @param key The entity description ID from which this entity was built
   * @param value An entity object
   */
  def map(key: IntWritable, value: EntityWritable, collector: OutputCollector[NullWritable, QuadWritable], reporter: Reporter) {
    val mapping = mappings(key.get())
    for(quad <- mapping.executeMapping(value)) {
      collector.collect(NullWritable.get(), new QuadWritable(quad))
      reporter.getCounter("LDIF Stats", "R2R nr. of quads output").increment(1)
    }
  }

  private def getMappings(conf: JobConf): IndexedSeq[LDIFMapping] = {
    HadoopHelper.getDistributedObject(conf, "mappings").asInstanceOf[IndexedSeq[LDIFMapping]]
  }
}