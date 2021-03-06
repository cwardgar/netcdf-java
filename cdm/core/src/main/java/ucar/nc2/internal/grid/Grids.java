package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.grid.GridAxisOffsetTimeRegular;

import java.util.ArrayList;
import java.util.Map;

/** static utilities */
class Grids {

  static GridAxis1D extractGridAxis1D(NetcdfDataset ncd, CoordinateAxis axis, GridAxis.DependenceType dependenceType) {
    GridAxis1D.Builder<?> builder;
    AxisType axisType = axis.getAxisType();
    if (axisType == AxisType.Time || axisType == AxisType.RunTime) {
      builder = GridAxis1DTime.builder(axis).setAxisType(axis.getAxisType());
    } else {
      builder = GridAxis1D.builder(axis).setAxisType(axis.getAxisType());
    }
    extractGridAxis1D(ncd, axis, builder, dependenceType);
    return builder.build();
  }

  private static void extractGridAxis1D(NetcdfDataset ncd, CoordinateAxis axis, GridAxis1D.Builder<?> builder,
      GridAxis.DependenceType dependenceTypeFromClassifier) {
    Preconditions.checkArgument(axis.getRank() < 2);
    CoordinateAxis1DExtractor extract = new CoordinateAxis1DExtractor(axis);

    GridAxis.DependenceType dependenceType;
    if (axis.isCoordinateVariable()) {
      dependenceType = GridAxis.DependenceType.independent;
    } else if (ncd.isIndependentCoordinate(axis)) { // is a coordinate alias
      dependenceType = dependenceTypeFromClassifier; // TODO not clear
      builder.setDependsOn(ImmutableList.of(axis.getDimension(0).getShortName()));
    } else if (axis.isScalar()) {
      dependenceType = GridAxis.DependenceType.scalar;
    } else {
      dependenceType = GridAxis.DependenceType.dependent;
      ArrayList<String> dependsOn = new ArrayList<>();
      for (Dimension d : axis.getDimensions()) { // LOOK axes may not exist
        dependsOn.add(d.makeFullName(axis));
      }
      builder.setDependsOn(dependsOn);
    }
    builder.setDependenceType(axis.isScalar() ? GridAxis.DependenceType.scalar : dependenceType);

    // Fix discontinuities in longitude axis. These occur when the axis crosses the date line.
    extract.correctLongitudeWrap();

    builder.setNcoords(extract.ncoords);
    if (extract.isRegular) {
      builder.setSpacing(GridAxis.Spacing.regularPoint);
      double ending = extract.start + extract.increment * extract.ncoords;
      builder.setGenerated(extract.ncoords, extract.start, ending, extract.increment);

    } else if (!extract.isInterval) {
      builder.setSpacing(GridAxis.Spacing.irregularPoint);
      builder.setValues(extract.coords);
      double starting = extract.edge[0];
      double ending = extract.edge[extract.ncoords - 1];
      double resolution = (extract.ncoords == 1) ? 0.0 : (ending - starting) / (extract.ncoords - 1);
      builder.setResolution(Math.abs(resolution));

    } else if (extract.boundsAreContiguous) {
      builder.setSpacing(GridAxis.Spacing.contiguousInterval);
      builder.setValues(extract.edge);
      double starting = extract.edge[0];
      double ending = extract.edge[extract.ncoords];
      double resolution = (ending - starting) / extract.ncoords;
      builder.setResolution(Math.abs(resolution));

    } else {
      builder.setSpacing(GridAxis.Spacing.discontiguousInterval);
      double[] bounds = new double[2 * extract.ncoords];
      int count = 0;
      for (int i = 0; i < extract.ncoords; i++) {
        bounds[count++] = extract.bound1[i];
        bounds[count++] = extract.bound2[i];
      }
      builder.setValues(bounds);
      double starting = bounds[0];
      double ending = bounds[2 * extract.ncoords - 1];
      double resolution = (extract.ncoords == 1) ? 0.0 : (ending - starting) / (extract.ncoords - 1);
      builder.setResolution(Math.abs(resolution));
    }

    if (builder instanceof GridAxis1DTime.Builder) {
      GridAxis1DTime.Builder<?> timeBuilder = (GridAxis1DTime.Builder<?>) builder;
      CoordinateAxis1DTimeExtractor extractTime = new CoordinateAxis1DTimeExtractor(axis, extract.coords);
      timeBuilder.setTimeHelper(extractTime.timeHelper);
      timeBuilder.setCalendarDates(extractTime.cdates);
    }
  }

  static GridAxisOffsetTimeRegular extractGridAxisOffset2D(CoordinateAxis axis, GridAxis.DependenceType dependenceType,
      Map<String, GridAxis> gridAxes) {
    Preconditions.checkArgument(axis.getAxisType() == AxisType.TimeOffset);
    Preconditions.checkArgument(axis.getRank() == 2);
    GridAxisOffsetTimeRegular.Builder<?> builder =
        GridAxisOffsetTimeRegular.builder(axis).setAxisType(axis.getAxisType()).setDependenceType(dependenceType);

    CoordinateAxis2DExtractor extract = new CoordinateAxis2DExtractor(axis);
    builder.setMidpoints(extract.getMidpoints());
    builder.setBounds(extract.getBounds());
    builder.setHourOffsets(extract.getHourOffsets());
    builder.setSpacing(extract.isInterval() ? GridAxis.Spacing.discontiguousInterval : GridAxis.Spacing.irregularPoint);

    Preconditions.checkNotNull(extract.getRuntimeAxisName());
    GridAxis runtime = gridAxes.get(extract.getRuntimeAxisName());
    Preconditions.checkNotNull(runtime, extract.getRuntimeAxisName());
    Preconditions.checkArgument(runtime instanceof GridAxis1DTime, extract.getRuntimeAxisName());
    Preconditions.checkArgument(runtime.getAxisType() == AxisType.RunTime, extract.getRuntimeAxisName());
    builder.setRuntimeAxis((GridAxis1DTime) runtime);

    return builder.build();
  }

  /** Standard sort on Coordinate Axes */
  public static class AxisComparator implements java.util.Comparator<GridAxis> {
    public int compare(GridAxis c1, GridAxis c2) {
      Preconditions.checkNotNull(c1);
      Preconditions.checkNotNull(c2);
      AxisType t1 = c1.getAxisType();
      AxisType t2 = c2.getAxisType();
      Preconditions.checkNotNull(t1);
      Preconditions.checkNotNull(t2);
      return t1.axisOrder() - t2.axisOrder();
    }
  }


}
