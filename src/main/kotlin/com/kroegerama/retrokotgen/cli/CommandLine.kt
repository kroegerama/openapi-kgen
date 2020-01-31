package com.kroegerama.retrokotgen.cli

import com.kroegerama.retrokotgen.Constants
import com.kroegerama.retrokotgen.Util
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