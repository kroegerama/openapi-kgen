package com.kroegerama.kgen.cli

import com.kroegerama.kgen.Constants
import com.kroegerama.kgen.OptionSet
import com.kroegerama.kgen.generator.Generator
import io.airlift.airline.Arguments
import io.airlift.airline.Command
import io.airlift.airline.Option

@Command(name = "generate", description = "Generate code from the specified OpenAPI Spec.")
class GenerateCommand : Runnable {

    @Option(
        name = ["-p", "--package-name"],
        title = "package name"
    )
    private val packageName: String = Constants.DEFAULT_PACKAGE_NAME

    @Option(
        name = ["-o", "--output"],
        title = "output directory",
        required = true
    )
    private val output = ""

    @Arguments(
        title = "spec file",
        description = "Spec file (yaml/json). Can be a file or url.",
        required = true
    )
    private val specFile = ""

    @Option(
        name = ["-l", "--limit-apis"],
        title = "limit apis",
        description = "If set, generate only these APIs (set via tag) and their models. Comma separated list. Example: \"auth,app\""
    )
    private val limitApis = ""

    @Option(
        name = ["-a", "--generate-all-named-schemas"],
        title = "Generate all named schemas, even those that would have been filtered via --limit-apis."
    )
    private val generateAllNamedSchemas = false

    @Option(
        name = ["-v", "--verbose"],
        title = "detailed output"
    )
    private val verbose = true

    @Option(
        name = ["--use-compose"],
        title = "Use compose",
        description = "Use compose immutable annotation for named data classes."
    )
    private val useCompose = false

    @Option(
        name = ["--allow-parse-errors"],
        title = "Allow parse errors",
        description = "Try to generate classes, even if parsing errors occur in the spec."
    )
    private val allowParseErrors = false

    override fun run() {
        val options = OptionSet(
            specFile = specFile,
            packageName = packageName,
            outputDir = output,
            limitApis = limitApis.split(",").filter { it.isNotBlank() }.toSet(),
            generateAllNamedSchemas = generateAllNamedSchemas,
            useCompose = useCompose,
            allowParseErrors = allowParseErrors,
            outputDirIsSrcDir = false,
            verbose = verbose
        )
        Generator(options).generate()
    }
}
