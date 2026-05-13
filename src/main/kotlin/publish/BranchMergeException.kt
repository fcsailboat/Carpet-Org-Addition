package publish

class BranchMergeException : RuntimeException {
    constructor(
        current: String,
        other: String,
        solution: String
    ) : super("[current=$current, other=$other] - $solution")
}