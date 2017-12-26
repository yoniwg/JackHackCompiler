package jackToVm.codeGen

import jackToVm.CodeLocation

class VmCodeGenerationException(codeLocation: CodeLocation, errMessage: String)
    : Exception("[${codeLocation.file} : ${codeLocation.lineNumber}]: $errMessage")