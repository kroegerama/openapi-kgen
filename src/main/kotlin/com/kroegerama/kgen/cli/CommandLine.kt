package com.kroegerama.kgen.cli

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.Util
import io.airlift.airline.Cli
import io.airlift.airline.Help

fun main(args: Array<String>) {
    Cli.CliBuilder<Runnable>(Constants.CLI_NAME)
        .withDescription(Util.generatorInfo)
        .withDefaultCommand(Help::class.java)
        .withCommands(
            Generate::class.java,
            Help::class.java
        )
        .build()
        .parse(*args)
        .run()
}