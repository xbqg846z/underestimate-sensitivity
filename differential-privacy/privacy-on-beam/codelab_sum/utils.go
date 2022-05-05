package main

import (
	"github.com/apache/beam/sdks/v2/go/pkg/beam/io/textio"
	"github.com/apache/beam/sdks/v2/go/pkg/beam"
)

func readInput(s beam.Scope, input string) beam.PCollection {
	s = s.Scope("readInput")
	lines := textio.Read(s, input)
	return beam.ParDo(s, CreatePairFn, lines)
}

func writeOutput(s beam.Scope, output beam.PCollection, outputTextName string) {
	s = s.Scope("writeOutput")
	output = beam.ParDo(s, convertToPairFn, output)
	formattedOutput := beam.Combine(s, &normalizeOutputCombineFn{}, output)
	textio.Write(s, outputTextName, formattedOutput)
}
