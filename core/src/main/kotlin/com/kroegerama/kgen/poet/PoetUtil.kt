package com.kroegerama.kgen.poet

import com.squareup.kotlinpoet.ParameterSpec

data class ParameterSpecPairInfo(
    val ifaceParam: ParameterSpec,
    val delegateParam: ParameterSpec
)