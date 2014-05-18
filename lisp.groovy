kLPar = '('
kRPar = ')'
kQuote = "'"
kNil = [tag: 'nil', data: 'nil']

def safeCar(obj) {
  if (obj['tag'] == 'cons') {
    return obj['car']
  }
  return kNil
}

def safeCdr(obj) {
  if (obj['tag'] == 'cons') {
    return obj['cdr']
  }
  return kNil
}

def makeError(str) {
  return [tag: 'error', data: str]
}

sym_table = [:]
def makeSym(str) {
  if (str == 'nil') {
    return kNil
  } else if (!sym_table[str]) {
    sym_table[str] = [tag: 'sym', data: str]
  }
  return sym_table[str]
}

def makeNum(num) {
  return [tag: 'num', data: num]
}

def makeCons(a, d) {
  return [tag: 'cons', car: a, cdr: d]
}

def makeSubr(fn) {
  return [tag: 'subr', data: fn]
}

def makeExpr(args, env) {
  return [tag: "expr", args: safeCar(args), body: safeCdr(args), env: env]
}

def isDelimiter(c) {
  return c == kLPar || c == kRPar || c == kQuote || c ==~ /\s+/
}

def skipSpaces(str) {
  return str.replaceFirst(/^\s+/, '')
}

def makeNumOrSym(str) {
  if (str ==~ /^-?\d+$/) {
    return makeNum(str.toInteger())
  }
  return makeSym(str)
}

def readAtom(str) {
  next = ''
  for (i = 0; i < str.size(); i++) {
    if (isDelimiter(str[i])) {
      next = str[i..-1]
      str = str[0..i-1]
      break
    }
  }
  return [makeNumOrSym(str), next]
}

def read(str) {
  str = skipSpaces(str)
  if (str == '') {
    return makeError('empty input')
  } else if (str[0] == kRPar) {
    return makeError('invalid syntax: ' + str)
  } else if (str[0] == kLPar) {
    return makeError('noimpl')
  } else if (str[0] == kQuote) {
    return makeError('noimpl')
  }
  return readAtom(str)
}

while (line = System.console().readLine('> ')) {
  println(read(line))
}
