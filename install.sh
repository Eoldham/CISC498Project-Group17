#!/bin/bash

# Put the absolute or relative path to the plugins folder of your ImageJ/Fiji installation here.
imageJ=../../fiji-win64/Fiji.app/plugins

mvn clean package
cp target/classes/CalciumSignal_.class plugin
cp src/main/java/CalciumSignal_.java plugin
cd plugin
jar cvfM CalciumSignal_.jar CalciumSignal_.class CalciumSignal_.java imageJ/plugins/*.class plugins.config
cp CalciumSignal_.jar $imageJ