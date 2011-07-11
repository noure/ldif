package de.fuberlin.wiwiss.ldif.local

import ldif.module.Executor
import de.fuberlin.wiwiss.ldif.EntityBuilderTask
import ldif.local.runtime._

class EntityBuilderExecutor(configParameters: ConfigParameters = ConfigParameters(new ConfigProperties, null)) extends Executor {

  type TaskType = EntityBuilderTask
  type InputFormat = GraphFormat
  type OutputFormat = DynamicEntityFormat

  /**
   * Determines the accepted input format of a specific task.
   */
  override def input(task : EntityBuilderTask) : GraphFormat = GraphFormat()

  /**
   * Determines the output format of a specific task.
   */
  override def output(task : EntityBuilderTask) : DynamicEntityFormat = DynamicEntityFormat()

  /**
   * Executes a specific task.
   *
   * @param task The task to be executed
   * @param reader The reader of the input data
   * @param writer The writer of the output data
   */
  override def execute(task : EntityBuilderTask, reader : Seq[QuadReader], writer : Seq[EntityWriter])
  {
    val eb = EntityBuilderFactory.getEntityBuilder(configParameters, task.entityDescriptions, reader)

    for ((ed, i) <- task.entityDescriptions.zipWithIndex )
      eb.buildEntities(ed, writer(i))

  }
}
