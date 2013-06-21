#!/bin/bash
java -cp ".:dist/*:dist/jogl/*:dist/netcdf/*" nl.esciencecenter.visualization.ewatercycle.WaterCycleApp $@
