package jackToVm.codeGen

import jackToVm.CodeLocation

class VmCodeGenerationException(codeLocation: CodeLocation, errMessage: String, cause : Exception? = null)
    : Exception("[${codeLocation.file.name} : ${codeLocation.lineNumber}]: $errMessage", cause)

class TypeMismatchException(errMessage: String) : Exception(errMessage)