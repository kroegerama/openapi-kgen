package com.kroegerama.kgen.poet

class PoetGenerator :
    IBaseFilesGenerator by BaseFilesGenerator,
    IModelFilesGenerator by ModelFilesGenerator,
    IApiFilesGenerator by ApiFilesGenerator {



}