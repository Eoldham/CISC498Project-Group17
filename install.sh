#!/bin/bash

# Put the absolute or relative path to the plugins folder of your ImageJ/Fiji installation here.
# If relative, make sure it's relative to the root directory of this project.
imageJ=../fiji-win64/Fiji.app/plugins

mvn clean package
cp target/classes/CalciumSignal_.class plugin
cp src/main/java/CalciumSignal_.java plugin
cd plugin
jar cvfM CalciumSignal_.jar CalciumSignal_.class CalciumSignal_.java imageJ/plugins/*.class plugins.config
cd ..
cp plugin/CalciumSignal_.jar $imageJ