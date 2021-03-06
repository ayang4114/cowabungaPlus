use io
use conv

printArray(a: int[]) {
    i: int = 0
    comma: int[] = ",";
    print("{")
    while i < length(a) {
        print(unparseInt(a[i]))
        print(comma);
        i = i + 1
    }
    println("}")
}

swap(a: int[], i: int, j: int) {
    temp: int = a[i]
    a[i] = a[j]
    a[j] = temp
}

main(args: int[][]) {
    arr: int[] = parseArgs(args);
    sort(arr, 0, length(arr) - 1)
    printArray(arr)
}

sort(a: int[], lo: int, hi: int) {
    if lo < hi {
        p: int = pivot(a, lo, hi)
        sort(a, lo, p - 1)
        sort(a, p + 1, hi)
    }
}

pivot(a: int[], lo: int, hi: int): int {
    p: int = a[hi]
    i: int = lo
    j: int = lo
    while j <= hi {
        if a[j] < p {
            swap(a, i, j)
            i = i + 1
        }
        j = j + 1
    }
    swap(a, i, hi)
    return i
}

parseArgs(args: int[][]): int[] {
    size: int = length(args);
    arr: int[size - 1]
    i: int = 1
    while i < size {
        parsedInt: int, _ = parseInt(args[i])
        arr[i - 1] = parsedInt;
        i = i + 1
    }
    return arr;
}

