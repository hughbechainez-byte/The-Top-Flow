package com.davehq.thetopflow.rhyme

data class RhymeCandidate(
    val word: String,
    val bucket: RhymeBucket,
    val score: Int
)
