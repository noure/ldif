/* 
 * LDIF
 *
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

package ldif.local

import java.io.File
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ldif.entity.Node
import ldif.runtime.Quad
import ldif.util.OutputValidator
import ldif.output.SerializingQuadWriter
import ldif.config.{COMPLETE, IntegrationConfig}

@RunWith(classOf[JUnitRunner])
class IntegrationFlowIT extends FlatSpec with ShouldMatchers {

  it should "run the whole integration flow correctly" in {
    // Run LDIF
    val configFile = loadConfig("ldif/local/resources/config.xml")
    val ldifOutput = runLdif(configFile, true)

    // Load results to compare with
    val ldimporterOuputUrl = getClass.getClassLoader.getResource("ldif/local/resources/results.nq")
    val ldimporterOuputFile = new File(ldimporterOuputUrl.toString.stripPrefix("file:"))

    // quantity check
    //Source.fromFile(ldifOutput).getLines.size should equal(4835)
    // quality check
    OutputValidator.compare(ldifOutput,ldimporterOuputFile, true) should equal (0)
  }

  it should "handle rewriting and provenance correctly" in {
    // Run LDIF
    val configFile = loadConfig("ldif/local/resources/config-provenance.xml")
    val ldifOutput = runLdif(configFile)

    // Create provenance quads to look for
    val provenanceQuads = List(
      Quad(Node.createUriNode("aba"),
        "http://www4.wiwiss.fu-berlin.de/ldif/ldif.owl#lastmod",
        Node.createTypedLiteral("2011-01-01T01:00:00.000+01:00","http://www.w3.org/2001/XMLSchema#dateTime"),
        "http://www4.wiwiss.fu-berlin.de/ldif/provenance"),
      Quad(Node.createUriNode("http://brain-map.org/mouse/brain/Chrna7.xml"),
        "http://mywiki/resource/property/MgiMarkerAccessionId",
        Node.createTypedLiteral("MGI:99779","http://www.w3.org/2001/XMLSchema#string"),
        "aba"))

    // quantity check
    //Source.fromFile(ldifOutput).getLines.size should equal(15205)
    // quality check
    OutputValidator.contains(ldifOutput, provenanceQuads) should equal (true)
  }

  it should "handle entity descriptions without restriction correctly" in {
    // Run LDIF
    val configFile = loadConfig("ldif/local/resources/minimal/integrationJob.xml")
    val ldifOutput = runLdif(configFile)

    // Create provenance quads to look for
    val validQuads = List(
      Quad(Node.createUriNode("http://dbpedia.org/resource/Beat_It"),
        "http://www.w3.org/2000/01/rdf-schema#label",
        Node.createLanguageLiteral("Beat It","sl"),
        "http://www4.wiwiss.fu-berlin.de/ldif/graph#dbpedia.1"))

    val invalidQuad = List(
      Quad(Node.createUriNode("http://dbpedia.org/resource/Beat_It"),
        "http://www.w3.org/2000/01/rdf-schema#label",
        Node.createUriNode("http://dbpedia.org/resource/Michael_Jackson"),
        "http://www4.wiwiss.fu-berlin.de/ldif/graph#dbpedia.1"))

    OutputValidator.contains(ldifOutput, validQuads) should equal (true)
    OutputValidator.contains(ldifOutput, invalidQuad) should equal (false)
  }

//  it should "be correct with TDB backend" in {
//// Run LDIF
//    val configFile = loadConfig("ldif/local/resources/config-tdb.xml")
//    val ldifOutput = runLdif(configFile)
//
//    // Load results to compare with
//    val ldimporterOuputUrl = getClass.getClassLoader.getResource("ldif/local/resources/results.nt")
//    val ldimporterOuputFile = new File(ldimporterOuputUrl.toString.stripPrefix("file:"))
//
//    // quality check
//    OutputValidator.compare(ldifOutput,ldimporterOuputFile) should equal (0)
//  }


  protected def loadConfig(config : String) =  {

    val configUrl = getClass.getClassLoader.getResource(config)
    new File(configUrl.toString.stripPrefix("file:"))
  }

  protected def runLdif(configFile : File, debugMode: Boolean = false) = {
    val integrator = new IntegrationJob(IntegrationConfig.load(configFile), debugMode)
    integrator.runIntegration
    new File(integrator.config.outputs.getByPhase(COMPLETE).head.asInstanceOf[SerializingQuadWriter].filepath)
  }

}