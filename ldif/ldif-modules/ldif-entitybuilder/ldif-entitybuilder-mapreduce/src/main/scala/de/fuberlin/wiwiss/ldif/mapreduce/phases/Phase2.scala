package de.fuberlin.wiwiss.ldif.mapreduce.phases

import de.fuberlin.wiwiss.ldif.mapreduce.mappers._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapred._
import lib.{NullOutputFormat, MultipleOutputs}
import org.apache.hadoop.util._
import org.apache.hadoop.conf._
import org.apache.commons.io.FileUtils
import org.apache.hadoop.io.IntWritable
import ldif.mapreduce.types._
import java.io.File
import de.fuberlin.wiwiss.ldif.mapreduce.io._
import ldif.mapreduce.utils.HadoopHelper
import ldif.entity.{EntityDescription, EntityDescriptionMetadata, EntityDescriptionMetaDataExtractor}
import java.util.logging.Logger

/**
 *  Hadoop EntityBuilder - Phase 2
 *  Filtering quads and creating initial value paths
 **/

class Phase2 extends Configured with Tool {
  def run(args: Array[String]): Int = {
    val conf = getConf
    val job = new JobConf(conf, classOf[Phase2])

    //    job.setJarByClass(classOf[RunHadoop])
    job.setNumReduceTasks(0)
    job.setMapperClass(classOf[ProcessQuadsMapper])
    job.setMapOutputKeyClass(classOf[IntWritable])
    job.setMapOutputValueClass(classOf[ValuePathWritable])
    job.setOutputKeyClass(classOf[IntWritable])
    job.setOutputValueClass(classOf[ValuePathWritable])
    job.setOutputFormat(classOf[NullOutputFormat[IntWritable, ValuePathWritable]])

    MultipleOutputs.addNamedOutput(job, "seq", classOf[JoinValuePathMultipleSequenceFileOutput], classOf[IntWritable], classOf[ValuePathWritable])
    MultipleOutputs.addNamedOutput(job, "text", classOf[JoinValuePathMultipleTextFileOutput], classOf[IntWritable], classOf[ValuePathWritable])

    val in = new Path(args(0))
    val out = new Path(args(1))
    FileInputFormat.addInputPath(job, in)
    FileOutputFormat.setOutputPath(job, out)

    JobClient.runJob(job)

    return 0
  }
}

object Phase2 {
  private val log = Logger.getLogger(getClass.getName)

  def runPhase(in : String, out : String, entityDescriptions : Seq[EntityDescription]) : Int = {
    val edmd = (new EntityDescriptionMetaDataExtractor).extract(entityDescriptions)
    runPhase(in,out,edmd)
  }

  def runPhase(in : String, out : String, edmd : EntityDescriptionMetadata) : Int = {
    log.info("Starting phase 2 of the EntityBuilder: Filtering quads and creating initial value paths")

    FileUtils.deleteDirectory(new File(out))

    val start = System.currentTimeMillis
    val conf = new Configuration
    HadoopHelper.distributeSerializableObject(edmd, conf, "edmd")

    val res = ToolRunner.run(conf, new Phase2(), Array(in, out))

    log.info("That's it. Took " + (System.currentTimeMillis-start)/1000.0 + "s")
    res
  }
}