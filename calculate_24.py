import itertools
import operator

def solve24(nums):
    """
    Attempts to find an arithmetic expression using the numbers in nums
    that evaluates to 24.
    """
    ops = {
        '+': operator.add,
        '-': operator.sub,
        '*': operator.mul,
        '/': operator.truediv,
    }
    op_symbols = list(ops.keys())

    # Generate permutations of numbers
    for num_perm in set(itertools.permutations(nums)):
        # Generate combinations of operators (3 operators needed for 4 numbers)
        for op_perm in itertools.product(op_symbols, repeat=3):
            # Try different parenthesis structures
            # Structure 1: (a op1 b) op2 (c op3 d)
            try:
                val1 = ops[op_perm[0]](num_perm[0], num_perm[1])
                val2 = ops[op_perm[2]](num_perm[2], num_perm[3])
                if abs(ops[op_perm[1]](val1, val2) - 24) < 1e-6: # Use tolerance for float comparison
                    return f"({num_perm[0]} {op_perm[0]} {num_perm[1]}) {op_perm[1]} ({num_perm[2]} {op_perm[2]} {num_perm[3]}) = 24"
            except ZeroDivisionError:
                pass

            # Structure 2: ((a op1 b) op2 c) op3 d
            try:
                val1 = ops[op_perm[0]](num_perm[0], num_perm[1])
                val2 = ops[op_perm[1]](val1, num_perm[2])
                if abs(ops[op_perm[2]](val2, num_perm[3]) - 24) < 1e-6:
                    return f"(({num_perm[0]} {op_perm[0]} {num_perm[1]}) {op_perm[1]} {num_perm[2]}) {op_perm[2]} {num_perm[3]} = 24"
            except ZeroDivisionError:
                pass

            # Structure 3: a op1 ((b op2 c) op3 d) - Less common but possible
            try:
                val1 = ops[op_perm[1]](num_perm[1], num_perm[2])
                val2 = ops[op_perm[2]](val1, num_perm[3])
                if abs(ops[op_perm[0]](num_perm[0], val2) - 24) < 1e-6:
                     return f"{num_perm[0]} {op_perm[0]} (({num_perm[1]} {op_perm[1]} {num_perm[2]}) {op_perm[2]} {num_perm[3]}) = 24"
            except ZeroDivisionError:
                pass

            # Structure 4: (a op1 (b op2 c)) op3 d
            try:
                val1 = ops[op_perm[1]](num_perm[1], num_perm[2])
                val2 = ops[op_perm[0]](num_perm[0], val1)
                if abs(ops[op_perm[2]](val2, num_perm[3]) - 24) < 1e-6:
                    return f"({num_perm[0]} {op_perm[0]} ({num_perm[1]} {op_perm[1]} {num_perm[2]})) {op_perm[2]} {num_perm[3]} = 24"
            except ZeroDivisionError:
                pass

            # Structure 5: a op1 (b op2 (c op3 d))
            try:
                val1 = ops[op_perm[2]](num_perm[2], num_perm[3])
                val2 = ops[op_perm[1]](num_perm[1], val1)
                if abs(ops[op_perm[0]](num_perm[0], val2) - 24) < 1e-6:
                    return f"{num_perm[0]} {op_perm[0]} ({num_perm[1]} {op_perm[1]} ({num_perm[2]} {op_perm[2]} {num_perm[3]})) = 24"
            except ZeroDivisionError:
                pass

    return "No solution found."

# Numbers provided by the user
numbers = [5, 5, 5, 1]
solution = solve24(numbers)
print(solution)