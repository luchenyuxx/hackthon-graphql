name := "graphql"

version := "0.1"

scalaVersion := "2.12.8"

// For cancelable processes in interactive shell
cancelable in Global := true

// To close the server stream when we run it in interactive shell
fork in run := true
fork in Test := true

// For correct test ordering in integration tests
parallelExecution in Test := true
parallelExecution in IntegrationTest := false

// Inspired by https://tpolecat.github.io/2017/04/25/scalac-flags.html
scalacOptions ++= Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match",              // Pattern match may not be typesafe.
  "-Ypartial-unification",             // Enable partial unification in type constructor inference
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  "-Ywarn-unused:params",              // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
)

lazy val versions = new {
  val cats = "1.5.0"
  val catsEffect = "1.1.0"
  val circe = "0.10.0"
  val http4s = "0.20.0"
  val logback = "1.2.3"
  val mockito = "1.0.6"
  val pureConfig = "0.10.1"
  val scalaLogging = "3.9.0"
  val scalaTest = "3.0.5"
  val sangria = "1.4.2"
}

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    buildInfoKeys := Seq[BuildInfoKey](version, git.gitHeadCommit),
    buildInfoPackage := "buildinfo",
    libraryDependencies ++= Seq(
      "org.typelevel"                %% "cats-core"                      % versions.cats,
      "org.typelevel"                %% "cats-effect"                    % versions.catsEffect,
      "io.circe"                     %% "circe-generic"                  % versions.circe,
      "io.circe"                     %% "circe-parser"                   % versions.circe,
      "io.circe"                     %% "circe-generic-extras"           % versions.circe,
      "org.http4s"                   %% "http4s-async-http-client"       % versions.http4s,
      "org.http4s"                   %% "http4s-blaze-server"            % versions.http4s,
      "org.http4s"                   %% "http4s-circe"                   % versions.http4s,
      "org.http4s"                   %% "http4s-dsl"                     % versions.http4s,
      "org.http4s"                   %% "http4s-prometheus-metrics"      % versions.http4s,
      "ch.qos.logback"                % "logback-classic"                % versions.logback,
      "com.github.pureconfig"        %% "pureconfig"                     % versions.pureConfig,
      "com.typesafe.scala-logging"   %% "scala-logging"                  % versions.scalaLogging,
      "org.sangria-graphql"          %% "sangria"                        % versions.sangria,
      "org.sangria-graphql"          %% "sangria-circe"                  % "1.2.1",
      "com.47deg"                    %% "fetch"                          % "1.0.0",


// Tests dependencies
      "org.mockito"                  %% "mockito-scala"                  % versions.mockito % "test",
      "org.scalatest"                %% "scalatest"                      % versions.scalaTest % "it,test"
    )
  )

lazy val assemblyFolder = file("assembly")
lazy val ignoreFiles = List("application.conf", "logback.xml", "META-INF/io.netty.versions.properties")

cleanFiles += assemblyFolder
test in assembly := {}
assemblyOutputPath in assembly := assemblyFolder / (name.value + "-" + version.value + ".jar")

// Remove resources files from the JAR (they will be copied to an external folder)
assemblyMergeStrategy in assembly := { path =>
  if (ignoreFiles.contains(path))
    MergeStrategy.discard
  else
    (assemblyMergeStrategy in assembly).value(path)
}
