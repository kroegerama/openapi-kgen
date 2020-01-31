package com.kroegerama.retrokotgen.poet

class PoetGenerator :
    IBaseFilesGenerator by BaseFilesGenerator,
    IModelFilesGenerator by ModelFilesGenerator,
    IApiFilesGenerator by ApiFilesGenerator {



}