package com.kroegerama.kgen.poet

import com.squareup.kotlinpoet.ParameterSpec

data class ParameterSpecPair(
    val ifaceParam: ParameterSpec,
    val delegateParam: ParameterSpec
)