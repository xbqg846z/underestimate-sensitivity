package main

import (
	"context"
	"fmt"
	"path"
	"reflect"
	"strings"

	"flag"
	log "github.com/golang/glog"
	"github.com/apache/beam/sdks/v2/go/pkg/beam"

	// The following import is required for accessing local files.
	_ "github.com/apache/beam/sdks/v2/go/pkg/beam/io/filesystem/local"

	"github.com/apache/beam/sdks/v2/go/pkg/beam/runners/direct"
)


func init() {
	beam.RegisterType(reflect.TypeOf((*normalizeOutputCombineFn)(nil)))
	beam.RegisterType(reflect.TypeOf(outputAccumulator{}))
	beam.RegisterFunction(convertToPairFn)
}

var (
	inputFile       = flag.String("input_file", "", "Input csv file name with raw data.")
	outputStatsFile = flag.String("output_stats_file", "", "Output csv file name for stats results.")
)

func main() {
	flag.Parse()

	// beam.Init() is an initialization hook that must be called on startup. On
	// distributed runners, it is used to intercept control.
	beam.Init()

	// Flag validation.
	if *inputFile == "" {
		log.Exit("No input file specified.")
	}
	if *outputStatsFile == "" {
		log.Exit("No output stats file specified.")
	}

	// Create a pipeline.
	p := beam.NewPipeline()
	s := p.Root()

	// Read and parse the input.
	col := readInput(s, *inputFile)

	// Run the example pipeline.
	dpOutput := privateSum(s, col)

	// Write the text output to file.
	log.Info("Writing text output.")
	writeOutput(s, dpOutput, outputStatsFile)

	// Execute pipeline.
	_, err := direct.Execute(context.Background(), p)
	if err != nil {
		log.Exitf("Execution of pipeline failed: %v", err)
	}
}

func convertToPairFn(k int, v beam.V) (pair, error) {
	switch v := v.(type) {
	case int:
		return pair{K: k, V: float64(v)}, nil
	case int64:
		return pair{K: k, V: float64(v)}, nil
	case float64:
		return pair{K: k, V: v}, nil
	default:
		return pair{}, fmt.Errorf("expected int, int64 or float64 for value type, got %v", v)
	}
}

type outputAccumulator struct {
	acc float64
}

type normalizeOutputCombineFn struct{}

func (fn *normalizeOutputCombineFn) CreateAccumulator() outputAccumulator {
	return outputAccumulator{acc: 0.}
}

func (fn *normalizeOutputCombineFn) AddInput(a outputAccumulator, p pair) outputAccumulator {
	// should never be called
	a.acc += p.V
	return a
}

func (fn *normalizeOutputCombineFn) MergeAccumulators(a, b outputAccumulator) outputAccumulator {
	return outputAccumulator{acc: a.acc + b.acc}
}

func (fn *normalizeOutputCombineFn) ExtractOutput(a outputAccumulator) string {
	return fmt.Sprintf("%f", a.acc)
}
