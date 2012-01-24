/* 
 * Copyright 2011-2012 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
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

package ldif.modules.sieve.local

import ldif.module.Executor
import ldif.local.runtime._
import impl.NoEntitiesLeft
import org.apache.commons.io.FileUtils
import ldif.local.util.TemporaryFileCreator
import xml.{XML, Source}
import org.slf4j.LoggerFactory
import java.io.{FileInputStream, File}
import ldif.runtime.Quad
import ldif.util.Prefixes
import ldif.entity.{Node, Entity, EntityDescription}
import ldif.modules.sieve.fusion.functions.{PassItOn, TrustYourFriends, KeepFirst}
import ldif.modules.sieve.fusion.{FusionConfig, FusionTask, FusionFunction}

/**
 * Executes Sieve Data Fusion on a local machine.
 * @author pablomendes - based on Silk and R2R executors.
 */
class SieveLocalFusionExecutor(useFileInstanceCache: Boolean = false) extends Executor
{
  private val log = LoggerFactory.getLogger(getClass.getName)

  //private val numThreads = 8
  //private val numThreads = Runtime.getRuntime.availableProcessors

  type TaskType = FusionTask

  type InputFormat = StaticEntityFormat

  type OutputFormat = GraphFormat

  def input(task : FusionTask) : InputFormat =
  {
    implicit val prefixes = task.sieveConfig.sieveConfig.prefixes
    //log.info("Prefixes:"+prefixes.toString)

    // here we create entity descriptions from the task.fusionSpec
    val entityDescriptions = task.sieveConfig.sieveConfig.entityDescriptions
    //val entityDescriptions = FusionConfig.createDummyEntityDescriptions(prefixes)

    new StaticEntityFormat(entityDescriptions)
  }

  def output(task : FusionTask) = new GraphFormat()

  private def getOrElse [T<:Object] (list: IndexedSeq[T], index: Int, default: T) : T = {
    if (index < list.size) {
      list(index)
    } else {
      default match {
        case f: FusionFunction => log.warn("No FusionFunction found for patternId=%d".format(index))
        case o: String => log.warn("No OutputProperty found for patternId=%d".format(index))
      }
      default
    }
  }

  /**
   * Executes a Sieve task.
   */
  override def execute(task : FusionTask, reader : Seq[EntityReader], writer : QuadWriter) {
    log.info("Executing Sieve Task %s".format(task.name))

    val quality = task.qualityAssessment

    // for each entity reader (one per input file?)
    reader.foreach( in => {
      val lastPatternId = in.entityDescription.patterns.size

      var entity : Entity = NoEntitiesLeft;
      while ( { entity = in.read(); entity != NoEntitiesLeft} ) {
        //log.info("Sieve Entity: %s".format(entity.resource.toString))
        //log.info("Patterns: "+in.entityDescription.patterns.size)

        assume(task.fusionSpec.fusionFunctions.size==in.entityDescription.patterns.size, "Number of scoringFunctions must be the same as number of patterns.")
        assume(task.fusionSpec.outputPropertyNames.size==in.entityDescription.patterns.size, "Number of outputPropertyNames must be the same as number of patterns.")

        if (entity==null) {
          log.error("Is it normal that some entities will be intermittently null? %s".format(in.entityDescription))
        }

        if (entity!=null && entity!=NoEntitiesLeft) {
          for (patternId <- 0 until lastPatternId) {
            val factums = entity.factums(patternId)
            val outputPropertyName = task.fusionSpec.outputPropertyNames(patternId)
            val fusionFunction = task.fusionSpec.fusionFunctions(patternId)
            //log.debug("Pattern %s: FusionFunction used: %s".format(patternId, fusionFunction))

            if (factums.size==1) { //nothing to fuse //TODO filtering functions apply to size=1 as well
              val patternNodes = factums.head
              val propertyValue = patternNodes(0)
              var graph = patternNodes(0).graph
              if (propertyValue.graph.trim().isEmpty) {
                println(patternNodes(0))
                graph = "PABLO"
              }
              val quad = new Quad(entity.resource, outputPropertyName, propertyValue, graph);
              writer.write(quad)
            } else {               // fuse multiple values into one value and write it out
              fusionFunction.fuse(factums, quality).foreach( patternNodes => { // for each property
                if (patternNodes.nonEmpty) {
                  val propertyValue = patternNodes(0) //TODO need to deal with case where the pattern is a tree (more than one path)
                  val quad = new Quad(entity.resource, outputPropertyName, propertyValue, propertyValue.graph);
                  writer.write(quad)
                }
              })
            }

          }
        }
      }
    })

  }

}