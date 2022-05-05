package main

import (
	"fmt"
	"reflect"
	"regexp"
	"strconv"
	"strings"

	"github.com/apache/beam/sdks/v2/go/pkg/beam"
)

type pair struct {
	K int
	V float64
}

func init() {
	beam.RegisterType(reflect.TypeOf((*pair)(nil)))
	beam.RegisterFunction(CreatePairFn)
}

// CreateVisitsFn creates and emits a Visit struct from a line that holds visit information.
func CreatePairFn(line string, emit func(pair)) error {
	// Skip the column headers line
	notHeader, err := regexp.MatchString("[0-9]", line)
	if err != nil {
		return err
	}
	if !notHeader {
		return nil
	}

	cols := strings.Split(line, ",")
	if len(cols) != 2 {
		return fmt.Errorf("got %d number of columns in line %q, expected 2", len(cols), line)
	}
	uid, err := strconv.Atoi(cols[0])
	if err != nil {
		return err
	}
	val, err := strconv.ParseFloat(cols[1], 64)
	if err != nil {
		return err
	}
	emit(pair{ K: uid, V: val })
	return nil
}
