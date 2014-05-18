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
  return [tag: 'expr', args: safeCar(args), body: safeCdr(args), env: env]
}

def nreverse(lst) {
  def ret = kNil
  while (lst['tag'] == 'cons') {
    def tmp = lst['cdr']
    lst['cdr'] = ret
    ret = lst
    lst = tmp
  }
  return ret
}

def pairlis(lst1, lst2) {
  def ret = kNil
  while (lst1['tag'] == 'cons' && lst2['tag'] == 'cons') {
    ret = makeCons(makeCons(lst1['car'], lst2['car']), ret)
    lst1 = lst1['cdr']
    lst2 = lst2['cdr']
  }
  return nreverse(ret)
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
  def next = ''
  for (i = 0; i < str.size(); i++) {
    if (isDelimiter(str[i])) {
      next = str[i..-1]
      str = str[0..i-1]
      break
    }
  }
  return [makeNumOrSym(str), next]
}

def rest(str, i) {
  // rest('a', 1) => '' ('a'[1..-1] causes an out of range error)
  if (str.size() <= i) {
    return ''
  }
  return str[i..-1]
}

def read(str) {
  str = skipSpaces(str)
  if (str == '') {
    return [makeError('empty input'), '']
  } else if (str[0] == kRPar) {
    return [makeError('invalid syntax: ' + str), '']
  } else if (str[0] == kLPar) {
    return readList(rest(str, 1))
  } else if (str[0] == kQuote) {
    def tmp = read(rest(str, 1))
    return [makeCons(makeSym('quote'), makeCons(tmp[0], kNil)), tmp[1]]
  }
  return readAtom(str)
}

def readList(str) {
  def ret = kNil
  while (true) {
    str = skipSpaces(str)
    if (str == '') {
      return [makeError('unfinished parenthesis'), '']
    } else if (str[0] == kRPar) {
      break
    }
    def tmp = read(str)
    def elm = tmp[0]
    def next = tmp[1]
    if (elm['tag'] == 'error') {
      return [elm, '']
    }
    ret = makeCons(elm, ret)
    str = next
  }
  return [nreverse(ret), rest(str, 1)]
}

def printObj(obj) {
  if (obj['tag'] == 'num' || obj['tag'] == 'sym' || obj['tag'] == 'nil') {
    return obj['data'].toString()
  } else if (obj['tag'] == 'error') {
    return '<error: ' + obj['data'] + '>'
  } else if (obj['tag'] == 'cons') {
    return printList(obj)
  } else if (obj['tag'] == 'subr' || obj['tag'] == 'expr') {
    return '<' + obj['tag'] + '>'
  }
  return '<unknown>'
}

def printList(obj) {
  def ret = ''
  def first = true
  while (obj['tag'] == 'cons') {
    if (first) {
      first = false
    } else {
      ret += ' '
    }
    ret += printObj(obj['car'])
    obj = obj['cdr']
  }
  if (obj['tag'] == 'nil') {
    return '(' + ret + ')'
  }
  return '(' + ret + ' . ' + printObj(obj) + ')'
}

def findVar(sym, env) {
  while (env['tag'] == 'cons') {
    def alist = env['car']
    while (alist['tag'] == 'cons') {
      if (alist['car']['car'].is(sym)) {
        return alist['car']
      }
      alist = alist['cdr']
    }
    env = env['cdr']
  }
  return kNil
}

g_env = makeCons(kNil, kNil)

def addToEnv(sym, val, env) {
  env['car'] = makeCons(makeCons(sym, val), env['car'])
}

def eval(obj, env) {
  def tag = obj['tag']
  if (tag == 'nil' || tag == 'num' || tag == 'error') {
    return obj
  } else if (tag == 'sym') {
    def bind = findVar(obj, env)
    if (bind.is(kNil)) {
      return makeError(obj['data'] + ' has no value')
    }
    return bind['cdr']
  }

  def op = safeCar(obj)
  def args = safeCdr(obj)
  if (op.is(makeSym('quote'))) {
    return safeCar(args)
  } else if (op.is(makeSym('if'))) {
    if (eval(safeCar(args), env).is(kNil)) {
      return eval(safeCar(safeCdr(safeCdr(args))), env)
    }
    return eval(safeCar(safeCdr(args)), env)
  } else if (op.is(makeSym('lambda'))) {
    return makeExpr(args, env)
  }
  return apply(eval(op, env), evlis(args, env), env)
}

def evlis(lst, env) {
  def ret = kNil
  while (lst['tag'] == 'cons') {
    def elm = eval(lst['car'], env)
    if (elm['tag'] == 'error') {
      return elm
    }
    ret = makeCons(elm, ret)
    lst = lst['cdr']
  }
  return nreverse(ret)
}

def progn(body, env) {
  def ret = kNil
  while (body['tag'] == 'cons') {
    ret = eval(body['car'], env)
    body = body['cdr']
  }
  return ret
}

def apply(fn, args, env) {
  if (fn['tag'] == 'error') {
    return fn
  } else if (args['tag'] == 'error') {
    return args
  } else if (fn['tag'] == 'subr') {
    return fn['data'](args)
  } else if (fn['tag'] == 'expr') {
    return progn(fn['body'], makeCons(pairlis(fn['args'], args), fn['env']))
  }
  return makeError('noimpl')
}

def subrCar = {
  return safeCar(safeCar(it))
}

def subrCdr = {
  return safeCdr(safeCar(it))
}

def subrCons = {
  return makeCons(safeCar(it), safeCar(safeCdr(it)))
}

addToEnv(makeSym('car'), makeSubr(subrCar), g_env)
addToEnv(makeSym('cdr'), makeSubr(subrCdr), g_env)
addToEnv(makeSym('cons'), makeSubr(subrCons), g_env)
addToEnv(makeSym('t'), makeSym('t'), g_env)

while (line = System.console().readLine('> ')) {
  println(printObj(eval(read(line)[0], g_env)))
}
