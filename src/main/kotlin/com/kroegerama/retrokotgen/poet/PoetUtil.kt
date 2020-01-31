package com.kroegerama.retrokotgen.poet

import com.squareup.kotlinpoet.ParameterSpec

data class ParameterSpecPair(
    val ifaceParam: ParameterSpec,
    val delegateParam: ParameterSpec
)