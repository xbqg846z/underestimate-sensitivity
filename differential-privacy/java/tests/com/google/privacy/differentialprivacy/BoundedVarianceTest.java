//
// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.privacy.differentialprivacy;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.Double.NaN;
import static java.lang.Math.pow;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.privacy.differentialprivacy.proto.SummaryOuterClass.MechanismType;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Tests the accuracy of {@link BoundedVariance}. The test mocks {@link Noise} instance which
 * generates zero noise.
 *
 * <p>Statistical and DP properties of the algorithm are tested in {@link
 * com.google.privacy.differentialprivacy.statistical.BoundedVarianceDpTest}.
 */
@RunWith(JUnit4.class)
public class BoundedVarianceTest {
  private static final double EPSILON = 1.0;
  private static final double DELTA = 0.123;
  @Rule public final MockitoRule mocks = MockitoJUnit.rule();
  @Mock private Noise noise;
  private BoundedVariance variance;

  @Before
  public void setUp() {
    when(noise.getMechanismType()).thenReturn(MechanismType.GAUSSIAN);
    // Mock the noise mechanism so that it does not add any noise.
    mockDoubleNoise(0);
    mockLongNoise(0);

    variance =
        BoundedVariance.builder()
            .epsilon(EPSILON)
            .delta(DELTA)
            .noise(noise)
            .maxPartitionsContributed(1)
            .maxContributionsPerPartition(1)
            .lower(1.0)
            .upper(9.0)
            .build();
  }

  @Test
  public void addEntry() {
    variance.addEntry(2.0);
    variance.addEntry(4.0);
    variance.addEntry(6.0);
    variance.addEntry(8.0);

    assertThat(variance.computeResult()).isEqualTo(5.0);
  }

  @Test
  public void addEntry_nan_ignored() {
    // Add NaN - no exception is thrown.
    variance.addEntry(NaN);
    // Add any values (let's say 7 and 9). Verify that the result is equal to their variance.
    variance.addEntry(7);
    variance.addEntry(9);
    assertThat(variance.computeResult()).isEqualTo(1.0);
  }

  @Test
  public void addEntry_calledAfterComputeResult_throwsException() {
    variance.computeResult();
    assertThrows(IllegalStateException.class, () -> variance.addEntry(0.0));
  }

  @Test
  public void addEntries() {
    variance.addEntries(ImmutableList.of(2.0, 4.0, 6.0, 8.0));
    assertThat(variance.computeResult()).isEqualTo(5.0);
  }

  @Test
  public void addEntries_calledAfterComputeResult_throwsException() {
    variance.computeResult();
    assertThrows(IllegalStateException.class, () -> variance.addEntries(ImmutableList.of(0.0)));
  }

  @Test
  public void computeResult_multipleCalls_throwsException() {
    variance.computeResult();
    assertThrows(IllegalStateException.class, () -> variance.computeResult());
  }

  // Input values are clamped to the upper and lower bounds.
  @Test
  public void addEntry_clampsInput() {
    variance =
        BoundedVariance.builder()
            .epsilon(EPSILON)
            .delta(DELTA)
            .noise(noise)
            .maxPartitionsContributed(1)
            .maxContributionsPerPartition(1)
            .lower(0.0)
            .upper(2.0)
            .build();

    variance.addEntry(-1.0); // will be clamped to 0
    variance.addEntry(1.0); // will not be clamped
    variance.addEntry(10.0); // will be clamped to 2

    assertThat(variance.computeResult())
        .isEqualTo(/* (0^2 + 1^2 + 2^2) / 3 - ((0 + 1 + 2) / 3)^2 */ 2.0 / 3.0);
  }

  @Test
  public void computeResult_singleInput_returnsZero() {
    variance.addEntry(3.0);
    assertThat(variance.computeResult()).isEqualTo(0.0);
  }

  @Test
  public void computeResult_noInput_returnsZero() {
    assertThat(variance.computeResult()).isEqualTo(0.0);
  }

  @Test
  public void computeResult_callsNoiseCorrectly() {
    int maxPartitionsContributed = 1;
    int maxContributionsPerPartition = 3;
    variance =
        BoundedVariance.builder()
            .epsilon(3.0)
            .delta(0.9)
            .noise(noise)
            .maxPartitionsContributed(maxPartitionsContributed)
            .maxContributionsPerPartition(maxContributionsPerPartition)
            .lower(1.0)
            .upper(9.0)
            .build();
    variance.addEntry(2.0);
    variance.addEntry(4.0);
    variance.computeResult();

    // Noising normalized sum of squares.
    verify(noise)
        .addNoise(
            eq(/* (x1 - midpoint)^2 + (x2 - midpoint)^2 = (2 - 5)^2 + (4 - 5)^2 */ 10.0),
            eq(maxPartitionsContributed),
            eq(
                /* maxContributionsPerPartition * ((upper - lower) / 2)^2 = 3 * ((9 - 1) / 2)^2 */ 48.0),
            eq(/* epsilon */ 1.0),
            doubleThat(closeTo(/* delta */ 0.3)));

    // Noising normalized sum.
    verify(noise)
        .addNoise(
            eq(/* x1 + x2 - midpoint * count = 2 + 4 - 5 * 2 */ -4.0),
            eq(maxPartitionsContributed),
            eq(/* maxContributionsPerPartition * (upper - lower) / 2 = 3 * (9 - 1) / 2 */ 12.0),
            eq(/* epsilon */ 1.0),
            doubleThat(closeTo(/* delta */ 0.3)));

    // Noising count.
    verify(noise)
        .addNoise(
            eq(/* count */ 2L),
            eq(maxPartitionsContributed),
            eq(
                /* sensitivity of count  = maxContributionsPerPartition*/ (long)
                    maxContributionsPerPartition),
            eq(/* epsilon */ 1.0),
            doubleThat(closeTo(/* delta */ 0.3)));
  }

  @Test
  public void computeResult_addsNoiseToSum() {
    // Mock the noise mechanism so that it adds noise to the both sums == -10.0.
    mockDoubleNoise(-10);
    // Mock the noise mechanism so that it adds noise to the count == 0.
    mockLongNoise(0);

    variance =
        BoundedVariance.builder()
            .epsilon(EPSILON)
            .delta(DELTA)
            .noise(noise)
            .maxPartitionsContributed(1)
            .maxContributionsPerPartition(1)
            .lower(-100)
            .upper(100)
            .build();

    variance.addEntry(10);
    variance.addEntry(10);
    // midpoint = (lower + upper) / 2 = 0,
    // noised_normalized_sum_of_squares = (x1 - midpoint)^2 + (x2 - midpoint)^2 + noise = 10^2 +
    // 10^2 - 10 = 190,
    // noised_normalized_sum = (x1 + x2) - midpoint * count + noise = 10 + 10 - 0 - 10 = 10,
    // noised_count = count + noise = 2 + 0 = 2,
    // BoundedVariance.computeResult() = noised_normalized_sum_of_squares / noised_count -
    // (noised_normalized_sum / noised_count)^2 = 190 / 2 - (10 / 2)^2 = 70
    assertThat(variance.computeResult()).isEqualTo(70.0);
  }

  @Test
  public void computeResult_addsNoiseToCount() {
    // Mock the noise mechanism so that it adds noise to the sums == 0.0.
    mockDoubleNoise(0);
    // Mock the noise mechanism so that it adds noise to the count == 2.
    mockLongNoise(2);

    variance =
        BoundedVariance.builder()
            .epsilon(EPSILON)
            .delta(DELTA)
            .noise(noise)
            .maxPartitionsContributed(1)
            .maxContributionsPerPartition(1)
            .lower(-100)
            .upper(100)
            .build();

    variance.addEntry(20);
    variance.addEntry(20);
    // midpoint = (lower + upper) / 2 = 0,
    // noised_normalized_sum_of_squares = (x1 - midpoint)^2 + (x2 - midpoint)^2 + noise = 20^2 +
    // 20^2 = 800,
    // noised_normalized_sum = (x1 + x2) - midpoint * count + noise = 20 + 20 - 0 + 0 = 40,
    // noised_count = count + noise = 2 + 2 = 4,
    // BoundedVariance.computeResult() = noised_normalized_sum_of_squares / noised_count -
    // (noised_normalized_sum / noised_count)^2 = 800 / 4 - (40 / 4)^2 = 100
    assertThat(variance.computeResult()).isEqualTo(100.0);
  }

  @Test
  public void computeResult_clampsTooHighVariance() {
    // We need to have non-zero noise to sum in order to get variance which needs to be clamped
    // (i.e., outside of the bounds). If no noise is added then the variance will always be within
    // the bounds because the input values are clamped.
    // The noise added to sums is 1.
    mockDoubleNoise(1);
    // The noise added to count is 0.
    mockLongNoise(0);

    variance =
        BoundedVariance.builder()
            .epsilon(EPSILON)
            .delta(DELTA)
            .noise(noise)
            .maxPartitionsContributed(1)
            .maxContributionsPerPartition(1)
            .lower(0.0)
            .upper(0.25)
            .build();

    variance.addEntry(0.25);
    variance.addEntry(0.25);
    // midpoint = (lower + upper) / 2 = 0.125,
    // noised_normalized_sum_of_squares = (x1 - midpoint)^2 + (x2 - midpoint)^2 + noise = 1.03125,
    // noised_normalized_sum = (x1 + x2) - midpoint * count + noise = 0.25 + 0.25 - 0.125 * 2 + 1 =
    // 1.25,
    // noised_count = count + noise = 2 + 0 = 2,
    // non_clamped_variance = noised_normalized_sum_of_squares / noised_count -
    // (noised_normalized_sum / noised_count)^2 = 1.03125 / 2 - (1.25 / 2)^2 = 0.125
    // BoundedVariance.computeResult = clamp(non_clamped_variance) = (0.25 - 0)^2 / 4 = 0.015625
    // (upper bound).
    assertThat(variance.computeResult()).isEqualTo(0.015625);
  }

  @Test
  public void computeResult_clampsTooLowVariance() {
    // We need to add non-zero noise to sums in order to get variance which needs to be clamped
    // (i.e., outside of the bounds). If no noise is added then the variance will always be within
    // the bounds because the input values are clamped.
    // The noise added to sums is -100.
    mockDoubleNoise(-100);
    // The noise added to count is 0.
    mockLongNoise(0);

    variance =
        BoundedVariance.builder()
            .epsilon(EPSILON)
            .delta(DELTA)
            .noise(noise)
            .maxPartitionsContributed(1)
            .maxContributionsPerPartition(1)
            .lower(0)
            .upper(10)
            .build();

    variance.addEntry(5.0);
    variance.addEntry(5.0);
    // midpoint = (lower + upper) / 2 = 5,
    // noised_normalized_sum_of_squares = (x1 - midpoint)^2 + (x2 - midpoint)^2 + noise = -100,
    // noised_normalized_sum = (x1 + x2) - midpoint * count + noise = 5 + 5 - 5 * 2 - 100 = -100,
    // noised_count = count + noise = 2 + 0 = 2,
    // non_clamped_variance = noised_normalized_sum_of_squares / noised_count -
    // (noised_normalized_sum / noised_count)^2 = -100 / 2 - (-100 / 2)^2 = -2550
    // BoundedVariance.computeResult = clamp(non_clamped_variance) = 0 (lower bound).
    assertThat(variance.computeResult()).isEqualTo(0.0);
  }

  /**
   * This test was designed to be not deterministic. It goes along with deterministic analogues in
   * order to ensure that they don't miss something.
   */
  @Test
  public void computeResult_resultAlwaysInsideProvidedBoundaries() {
    int datasetSize = 10;
    for (int i = 0; i < 100; ++i) {
      Random random = new Random();
      double lower = random.nextDouble() * 100;
      double upper = lower + random.nextDouble() * 100;

      variance =
          BoundedVariance.builder()
              .epsilon(EPSILON)
              .noise(new LaplaceNoise())
              .maxPartitionsContributed(1)
              .maxContributionsPerPartition(1)
              .lower(lower)
              .upper(upper)
              .build();

      List<Double> dataset =
          random
              .doubles()
              .map(x -> x * 300 * getRandomSign(random))
              .limit(datasetSize)
              .boxed()
              .collect(toImmutableList());

      variance.addEntries(dataset);

      assertWithMessage(
              "lower = %s\nupper = %s\ndataset = [%s]",
              lower, upper, dataset.stream().map(x -> Double.toString(x)).collect(joining(",\n")))
          .that(variance.computeResult())
          .isIn(Range.closed(0.0, (upper - lower) * (upper - lower) / 4.0));
    }
  }

  @Test
  public void getSerializableSummary_calledAfterComputeResult_throwsException() {
    variance.computeResult();

    assertThrows(IllegalStateException.class, () -> variance.getSerializableSummary());
  }

  @Test
  public void getSerializableSummary_multipleCalls_returnsSameSummary() {
    variance =
        BoundedVariance.builder()
            .epsilon(EPSILON)
            .noise(new LaplaceNoise())
            .maxPartitionsContributed(1)
            .maxContributionsPerPartition(1)
            .lower(0.0)
            .upper(1.0)
            .build();
    variance.addEntry(0.5);

    byte[] summary1 = variance.getSerializableSummary();
    byte[] summary2 = variance.getSerializableSummary();

    assertThat(summary1).isEqualTo(summary2);
  }

  @Test
  public void mergeWith_anotherValue_computesVarianceOfTwoValues() {
    BoundedVariance targetVariance = getVarianceBuilder().build();
    BoundedVariance sourceVariance = getVarianceBuilder().build();
    targetVariance.addEntry(1);
    sourceVariance.addEntry(9);

    targetVariance.mergeWith(sourceVariance.getSerializableSummary());

    assertThat(targetVariance.computeResult()).isEqualTo(16);
  }

  @Test
  public void mergeWith_calledTwice_computesVarianceOfAllValues() {
    BoundedVariance targetVariance = getVarianceBuilder().build();
    BoundedVariance sourceVariance1 = getVarianceBuilder().build();
    BoundedVariance sourceVariance2 = getVarianceBuilder().build();
    targetVariance.addEntry(1);
    sourceVariance1.addEntry(4);
    sourceVariance2.addEntry(7);

    targetVariance.mergeWith(sourceVariance1.getSerializableSummary());
    targetVariance.mergeWith(sourceVariance2.getSerializableSummary());

    assertThat(targetVariance.computeResult()).isEqualTo(6);
  }

  @Test
  public void mergeWith_epsilonMismatch_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().epsilon(EPSILON).build();
    BoundedVariance sourceVariance = getVarianceBuilder().epsilon(2 * EPSILON).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> targetVariance.mergeWith(sourceVariance.getSerializableSummary()));
  }

  @Test
  public void mergeWith_nullDelta_mergesWithoutException() {
    BoundedVariance targetVariance =
        getVarianceBuilder().noise(new LaplaceNoise()).delta(null).build();
    BoundedVariance sourceVariance =
        getVarianceBuilder().noise(new LaplaceNoise()).delta(null).build();

    // No exception should be thrown.
    targetVariance.mergeWith(sourceVariance.getSerializableSummary());
  }

  @Test
  public void mergeWith_deltaMismatch_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().delta(DELTA).build();
    BoundedVariance sourceVariance = getVarianceBuilder().delta(2 * DELTA).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> targetVariance.mergeWith(sourceVariance.getSerializableSummary()));
  }

  @Test
  public void mergeWith_noiseMismatch_throwsException() {
    BoundedVariance targetVariance =
        getVarianceBuilder().noise(new LaplaceNoise()).delta(null).build();
    BoundedVariance sourceVariance = getVarianceBuilder().noise(new GaussianNoise()).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> targetVariance.mergeWith(sourceVariance.getSerializableSummary()));
  }

  @Test
  public void mergeWith_maxPartitionsContributedMismatch_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().maxPartitionsContributed(1).build();
    BoundedVariance sourceVariance = getVarianceBuilder().maxPartitionsContributed(2).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> targetVariance.mergeWith(sourceVariance.getSerializableSummary()));
  }

  @Test
  public void mergeWith_differentMaxContributionsPerPartitionMismatch_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().maxContributionsPerPartition(1).build();
    BoundedVariance sourceVariance = getVarianceBuilder().maxContributionsPerPartition(2).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> targetVariance.mergeWith(sourceVariance.getSerializableSummary()));
  }

  @Test
  public void mergeWith_lowerBoundsMismatch_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().lower(-1).build();
    BoundedVariance sourceVariance = getVarianceBuilder().lower(-100).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> targetVariance.mergeWith(sourceVariance.getSerializableSummary()));
  }

  @Test
  public void mergeWith_upperBoundsMismatch_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().upper(1).build();
    BoundedVariance sourceVariance = getVarianceBuilder().upper(100).build();

    assertThrows(
        IllegalArgumentException.class,
        () -> targetVariance.mergeWith(sourceVariance.getSerializableSummary()));
  }

  @Test
  public void mergeWith_calledAfterComputeResult_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().build();
    BoundedVariance sourceVariance = getVarianceBuilder().build();
    targetVariance.computeResult();
    byte[] summary = sourceVariance.getSerializableSummary();

    assertThrows(IllegalStateException.class, () -> targetVariance.mergeWith(summary));
  }

  @Test
  public void mergeWith_calledAfterSerializationOnTargetVariance_throwsException() {
    BoundedVariance targetVariance = getVarianceBuilder().build();
    BoundedVariance sourceVariance = getVarianceBuilder().build();
    targetVariance.getSerializableSummary();
    byte[] summary = sourceVariance.getSerializableSummary();

    assertThrows(IllegalStateException.class, () -> targetVariance.mergeWith(summary));
  }

  private BoundedVariance.Params.Builder getVarianceBuilder() {
    return BoundedVariance.builder()
        .epsilon(EPSILON)
        .delta(DELTA)
        .noise(noise)
        .maxPartitionsContributed(1)
        // lower, upper and, maxContributionsPerPartition have arbitrarily chosen values.
        .maxContributionsPerPartition(10)
        .lower(-10)
        .upper(10);
  }

  private void mockDoubleNoise(double value) {
    when(noise.addNoise(anyDouble(), anyInt(), anyDouble(), anyDouble(), anyDouble()))
        .thenAnswer(invocation -> (double) invocation.getArguments()[0] + value);
  }

  private void mockLongNoise(long value) {
    when(noise.addNoise(anyLong(), anyInt(), anyLong(), anyDouble(), anyDouble()))
        .thenAnswer(invocation -> (long) invocation.getArguments()[0] + value);
  }

  private static int getRandomSign(Random random) {
    return random.nextBoolean() ? 1 : -1;
  }

  private static ArgumentMatcher<Double> closeTo(double expected) {
    return actual -> Math.abs(expected - actual) <= pow(10, -15);
  }
}
