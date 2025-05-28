import itertools
import operator
import sys

def solve24(nums):
    """
    Finds all arithmetic expressions using the numbers in nums
    that evaluate to 24.
    """
    solutions = [] # List to store all found solutions
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
                    solutions.append(f"({int(num_perm[0])} {op_perm[0]} {int(num_perm[1])}) {op_perm[1]} ({int(num_perm[2])} {op_perm[2]} {int(num_perm[3])}) = 24")
            except ZeroDivisionError:
                pass

            # Structure 2: ((a op1 b) op2 c) op3 d
            try:
                val1 = ops[op_perm[0]](num_perm[0], num_perm[1])
                val2 = ops[op_perm[1]](val1, num_perm[2])
                if abs(ops[op_perm[2]](val2, num_perm[3]) - 24) < 1e-6:
                    solutions.append(f"(({int(num_perm[0])} {op_perm[0]} {int(num_perm[1])}) {op_perm[1]} {int(num_perm[2])}) {op_perm[2]} {int(num_perm[3])} = 24")
            except ZeroDivisionError:
                pass

            # Structure 3: a op1 ((b op2 c) op3 d) - Less common but possible
            try:
                val1 = ops[op_perm[1]](num_perm[1], num_perm[2])
                val2 = ops[op_perm[2]](val1, num_perm[3])
                if abs(ops[op_perm[0]](num_perm[0], val2) - 24) < 1e-6:
                     solutions.append(f"{int(num_perm[0])} {op_perm[0]} (({int(num_perm[1])} {op_perm[1]} {int(num_perm[2])}) {op_perm[2]} {int(num_perm[3])}) = 24")
            except ZeroDivisionError:
                pass

            # Structure 4: (a op1 (b op2 c)) op3 d
            try:
                val1 = ops[op_perm[1]](num_perm[1], num_perm[2])
                val2 = ops[op_perm[0]](num_perm[0], val1)
                if abs(ops[op_perm[2]](val2, num_perm[3]) - 24) < 1e-6:
                    solutions.append(f"({int(num_perm[0])} {op_perm[0]} ({int(num_perm[1])} {op_perm[1]} {int(num_perm[2])})) {op_perm[2]} {int(num_perm[3])} = 24")
            except ZeroDivisionError:
                pass

            # Structure 5: a op1 (b op2 (c op3 d))
            try:
                val1 = ops[op_perm[2]](num_perm[2], num_perm[3])
                val2 = ops[op_perm[1]](num_perm[1], val1)
                if abs(ops[op_perm[0]](num_perm[0], val2) - 24) < 1e-6:
                    solutions.append(f"{int(num_perm[0])} {op_perm[0]} ({int(num_perm[1])} {op_perm[1]} ({int(num_perm[2])} {op_perm[2]} {int(num_perm[3])})) = 24")
            except ZeroDivisionError:
                pass

    # Return unique solutions
    return list(set(solutions))

if __name__ == "__main__":
    if len(sys.argv) != 5:
        print("Usage: python calculate_24.py num1 num2 num3 num4")
        sys.exit(1)

    try:
        # Convert arguments to numbers (float allows for division results)
        numbers = [float(arg) for arg in sys.argv[1:]]
    except ValueError:
        print("Error: All arguments must be numbers.")
        sys.exit(1)

    found_solutions = solve24(numbers)
    if found_solutions:
        print("Found solutions:")
        for sol in found_solutions:
            print(sol)
    else:
        print("No solution found.")