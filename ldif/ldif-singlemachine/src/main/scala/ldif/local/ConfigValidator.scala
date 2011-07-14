package ldif.local

import datasources.dump.DumpLoader
import de.fuberlin.wiwiss.r2r.{FileOrURISource, Repository}
import scala.collection.mutable.{Map, HashMap}
import ldif.local.datasources.dump.QuadFileLoader
import collection.JavaConversions
import java.util.logging.Logger
import java.io._

object ConfigValidator {
  val okMessage = "Ok"
  val log = Logger.getLogger(getClass.getName)
  val fileError = "Error in reading mapping file"
  val mappingsError = "Erroneous mappings found"

  def validateConfiguration(config: LdifConfiguration): Boolean = {
    var fail = false

    try {
      val r2rMappingsErrors = validateMappingFile(config.mappingFile)
      if(r2rMappingsErrors._1!="Ok")
        fail = true
      validateSilkLinkSpecs(config.linkSpecDir)
      if(configProperties.getPropertyValue("validateSources", "true").toLowerCase=="false") {
        println("-- Validation of source datasets disabled")
        if(fail)
          logErrors(r2rMappingsErrors, null)
        return fail
      }
      val sourceFileErrors = validateSourceFiles(config.sources)
      for (err <- sourceFileErrors)   {
        if(err._2.size > 0)
          fail = true
      }
      logErrors(r2rMappingsErrors, sourceFileErrors)
    } catch {
      case e: Exception => throw new RuntimeException("Unknown Error occured while validating configuration: " + e.getMessage, e)
    }
    return fail
  }

  def logErrors(r2rMappingErrors: (String, Map[String, String]), sourceFileErrors: Map[String, Seq[Pair[Int, String]]]) {
    if(r2rMappingErrors!=null)
      logR2RErrors(r2rMappingErrors)
    if(sourceFileErrors!=null)
      logSourceFileErrors(sourceFileErrors)
  }

  private def logR2RErrors(r2rMappingErrors: (String, Map[String, String])) {
    if(r2rMappingErrors._1=="Ok")
      return

    log.warning("Found R2R errors in configuration: " + r2rMappingErrors._1)
    if(r2rMappingErrors._2 != null)
      for((mapping, errorString) <- r2rMappingErrors._2)
        log.warning("Mapping <" + mapping + "> contains an error: " + errorString)
  }

  private def logSourceFileErrors(sourceErrors: Map[String, Seq[Pair[Int, String]]]) {
    for((source, errorMap) <- sourceErrors) {
      if(errorMap.size > 0) {
        val errorString = new StringBuilder()
        errorString.append("There have been ").append(errorMap.size).append(" errors in input source ").append(source).append(":")
        for((lineNr, line) <- errorMap)
          errorString.append("\n  ").append("In line ").append(lineNr).append(": ").append(line)
        log.warning(errorString.toString)
      }
    }
  }

  def validateSourceFiles(sources: Traversable[String]): Map[String, Seq[Pair[Int, String]]] = {
    val errorMap = new HashMap[String, Seq[Pair[Int, String]]]
    for(source <- sources) {
      try {
        val reader = new BufferedReader(new InputStreamReader(new DumpLoader(source).getStream))
        val loader = new QuadFileLoader
        val errors = loader.validateQuadsMT(reader)
        if(errors.size > 0)
          errorMap.put(source, errors)
      } catch {
        case e: IOException => errorMap.put(source, List(Pair(0, "Error reading file: " + e.getMessage)))
      }
    }
    return errorMap
  }

  def validateMappingFile(mappingFile: File): Pair[String, Map[String, String]] = {
    var mappingFileErrors: Pair[String, Map[String, String]] = null

    try {
      val repository = new Repository(new FileOrURISource(mappingFile))
      val erroneousMappings = JavaConversions.asScalaMap(repository.validateMappings)
      if(erroneousMappings.size>0)
        mappingFileErrors = Pair(mappingsError, erroneousMappings)
    } catch {
      case e: Exception => mappingFileErrors = Pair(fileError, null)
    }
    if(mappingFileErrors==null)
      mappingFileErrors = Pair("Ok", null)
    return mappingFileErrors
  }

  def validateSilkLinkSpecs(linkSpecsDir: File) {
    // TODO: Implement
  }
}