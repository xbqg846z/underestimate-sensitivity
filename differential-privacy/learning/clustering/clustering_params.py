# Copyright 2021 Google LLC.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Parameters for the clustering algorithm and data input to cluster."""

import dataclasses
import enum
import typing
from absl import logging

import numpy as np


@enum.unique
class PrivacyModel(enum.Enum):
  """Designates the model of privacy to use or simulate.

  CENTRAL : Uses the central model of privacy where a central aggregator has
  access to the data and only the output needs to be differentially private.
  """
  CENTRAL = "central"


@dataclasses.dataclass
class DifferentialPrivacyParam():
  """User parameters for differential privacy."""
  epsilon: float = 1.0
  delta: float = 1e-6
  privacy_model: PrivacyModel = PrivacyModel.CENTRAL


@dataclasses.dataclass
class PrivacyBudgetSplit():
  """How to split epsilon between the computations.

  Attributes:
    epsilon: Differential privacy parameter, epsilon, to be split.
    frac_sum: The fraction of epsilon to use when computing the sum of points
      for noisy averaging.
    frac_group_count: The fraction of epsilon to use when counting the number of
      points with a particular hash value.
  """
  frac_sum: float = 0.8
  frac_group_count: float = 0.2

  def __post_init__(self):
    total = self.frac_sum + self.frac_group_count
    if total > 1.0:
      raise ValueError(
          f"The provided privacy budget split ({total}) was greater than 1.0.")


@dataclasses.dataclass
class TreeParam():
  """Thresholds for constructing a tree.

  Attributes:
    min_num_points_in_branching_node: The minimum number of points that must be
      included for a node to branch.
    min_num_points_in_node: The minimum number of points that must be included
      to appear in the tree.
    max_depth: Maximum depth for the tree.
  """
  min_num_points_in_branching_node: int
  min_num_points_in_node: int
  max_depth: int

  def __post_init__(self):
    if self.min_num_points_in_node < 1:
      raise ValueError("Threshold for a node to be included in the tree must "
                       "be at least 1.")
    if self.min_num_points_in_branching_node < self.min_num_points_in_node:
      raise ValueError(
          "Threshold for branching should be at least the threshold for a node "
          "to be included in the tree.")


@dataclasses.dataclass
class ClusteringParam():
  """Parameters that determine the clustering algorithm.

  Attributes:
    privacy_param: privacy parameters for the algorithm
    privacy_budget_split: budget split between different computations
    tree_param: parameters for constructing LSH tree
    short_description: text description for clustering parameters.
    radius: Bound on the distance of each point from the origin in datapoints.
  """
  privacy_param: DifferentialPrivacyParam
  privacy_budget_split: PrivacyBudgetSplit
  tree_param: TreeParam
  short_description: typing.Optional[str]
  radius: float


# Numpy array where the rows are points.
Points = np.ndarray


@dataclasses.dataclass(frozen=True)
class Data():
  """Dataset and metadata needed by the clustering algorithm.

  Attributes:
    datapoints: Datapoints where each row is a datapoint.
    radius: Bound on the distance of each point from the origin in datapoints.
    labels: Labels, if any, for the data (None by default).
    num_points: Number of datapoints, populated based on the number of rows in
      datapoints (inferred from datapoints).
    dim: Dimension of each datapoint, populated based on the number of columns
      in datapoints (inferred from datapoints).
  """
  datapoints: Points
  radius: float
  labels: typing.Optional[np.ndarray] = None
  num_points: int = dataclasses.field(init=False)
  dim: int = dataclasses.field(init=False)

  def __post_init__(self):
    (num_points, dim) = np.shape(self.datapoints)
    object.__setattr__(self, "num_points", num_points)
    object.__setattr__(self, "dim", dim)
    if self.labels is not None and self.labels.shape[0] != self.num_points:
      raise ValueError(
          f"Number of labels ({self.labels.shape[0]}) is different from "
          f"number of datapoints ({self.num_points})")

  def clip_by_radius(self,
                     points: typing.Optional[Points] = None) -> np.ndarray:
    """Returns clipped points based on self.radius.

    If a point already has norm at most the radius, we leave it unchanged.
    Otherwise, we rescale it so that its norm is exactly radius.

    Args:
      points: Points to clip. If None, self.datapoints are used. Does not modify
        self.datapoints.
    """
    if points is None:
      points = self.datapoints
    scale = self.radius / np.maximum(
        np.linalg.norm(points, axis=-1), self.radius).reshape(-1, 1)
    if np.min(scale) < 1.0:
      logging.debug(
          "Found %s points outside of radius provided, rescaling them to "
          "have norm exactly equal to the radius.", np.sum(scale < 1.0))
    return points * scale
