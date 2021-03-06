/**
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.remote.artery

import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.ActorSystem

import scala.util.Try

/**
 * Simple to file logger for benchmark results. Will log relevant settings first to make sure
 * results can be understood later.
 */
trait BenchmarkFileReporter {
  def reportResults(result: String): Unit
  def close(): Unit
}
object BenchmarkFileReporter {
  val targetDirectory = {
    val target = new File("akka-stream-tests/target/benchmark-results")
    target.mkdirs()
    target
  }

  def apply(testName: String, system: ActorSystem): BenchmarkFileReporter =
    new BenchmarkFileReporter {
      val gitCommit = {
        import sys.process._
        Try("git describe".!!.trim).getOrElse("[unknown]")
      }
      val testResultFile: File = {
        val format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
        val fileName = s"${format.format(new Date())}-Artery-$testName-$gitCommit-results.txt"
        new File(targetDirectory, fileName)
      }
      val config = system.settings.config

      val fos = Files.newOutputStream(testResultFile.toPath)
      reportResults(s"Git commit: $gitCommit")

      val settingsToReport =
        Seq(
          "akka.test.MaxThroughputSpec.totalMessagesFactor",
          "akka.test.MaxThroughputSpec.real-message",
          "akka.test.LatencySpec.totalMessagesFactor",
          "akka.test.LatencySpec.repeatCount",
          "akka.test.LatencySpec.real-message",
          "akka.remote.artery.enabled",
          "akka.remote.artery.advanced.inbound-lanes",
          "akka.remote.artery.advanced.idle-cpu-level",
          "akka.remote.artery.advanced.buffer-pool-size",
          "akka.remote.artery.advanced.embedded-media-driver",
          "akka.remote.default-remote-dispatcher.throughput",
          "akka.remote.default-remote-dispatcher.fork-join-executor.parallelism-factor",
          "akka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min",
          "akka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max"
        )
      settingsToReport.foreach(reportSetting)

      def reportResults(result: String): Unit = synchronized {
        println(result)
        fos.write(result.getBytes("utf8"))
        fos.write('\n')
        fos.flush()
      }

      def reportSetting(name: String): Unit = {
        val value = if (config.hasPath(name)) config.getString(name) else "[unset]"
        reportResults(s"$name: $value")
      }

      def close(): Unit = fos.close()
    }
}
