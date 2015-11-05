/*
 *  Copyright 2014 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

(function () {
	'use strict';
	var retfunc = function (plotUtils) {

		var PlotTreeMapNode = function (data) {
			_(this).extend(data); // copy properties to itself
		};


		PlotTreeMapNode.prototype.isRoot = function () {
			return this.root === true;
		};

		PlotTreeMapNode.prototype.clearTips = function (scope) {
			//dummy function
		};

		PlotTreeMapNode.prototype.setHighlighted = function (scope, highlighted) {
			if (this.isRoot())
				return;
			var itemsvg = scope.maing.select("g").select("#" + this.id);
			itemsvg
				.transition()
				.duration(plotUtils.getHighlightDuration())
				.attr("filter", highlighted ? "url(#outerDropShadow)" : "")
				.select("rect")
				.style("stroke", highlighted ? "#000000" : "#FFFFFF")
				.style("stroke-width", highlighted ? 2 : 0.2)
			;
		};


		PlotTreeMapNode.prototype.prepare = function (scope) {
			if (!this.isRoot())
				return;

			var margin = {top: 0, right: 0, bottom: 0, left: 0},
				width = (scope ? scope.jqsvg.width() : 300) - margin.left - margin.right,
				height = (scope ? scope.jqsvg.height() : 200) - margin.top - margin.bottom;

			var treemap = d3.layout.treemap()
				.round(false)
				.size([width, height])
				.sticky(true)
				.value(function (d) {
					return d.showItem === true ? scope.stdmodel.valueAccessor === 'VALUE' ? d.doubleValue : d.weight : 0;
				});

			if (scope.stdmodel.mode) {
				treemap.mode(scope.stdmodel.mode)
			}
			if (scope.stdmodel.sticky) {
				treemap.sticky(scope.stdmodel.sticky)
			}
			if (scope.stdmodel.ratio) {
				treemap.ratio(scope.stdmodel.ratio)
			}
			if (scope.stdmodel.round) {
				treemap.round(scope.stdmodel.round)
			}

			var old_data = this.nodes;

			this.nodes = treemap
				.nodes(this)
				.filter(function (d) {
					return !d.children || d.children.length === 0;
				});
		};


		PlotTreeMapNode.prototype.render = function (scope) {
			if (!this.isRoot())
				return;

			this.clear(scope);
			if (scope.showAllItems) {
				var hasVisible = false;
				var visitor = {

					visit: function (node) {
						if (!node.children && node.showItem === true)
							hasVisible = true;
					}
				};
				scope.stdmodel.process(visitor);
				if (hasVisible === true) {
					this.draw(scope);
				}
			}
		};


		PlotTreeMapNode.prototype.draw = function (scope) {

			if (!this.isRoot())
				return;

			this.prepare(scope);
			var zoom = d3.behavior.zoom()
				.scaleExtent([1, 10])
				.on("zoom", zoomed);

			scope.maing
				.call(zoom)
			;

			var svg = scope.maing.append("svg:g");
			var cell = svg.selectAll("g")
					.data(this.nodes)
					.enter().append('svg:g')
					.attr('class', 'cell')
					.attr("id", function (d) {
						return d.id;
					})
					.attr('transform', function (d) {
						return 'translate(' + d.x + ',' + d.y + ')';
					})
					.on("mouseover", function (d) {
						if (scope.stdmodel.useToolTip === true && d.tooltip) {
							scope.tooltip.style("visibility", "visible");
							scope.tooltip.transition().duration(200).style("opacity", 0.9);
						}
					})
					.on("mousemove", function (d) {
						var xPosition = d3.event.layerX + 2;
						var yPosition = d3.event.layerY - 2;

						scope.tooltip
							.style("left", xPosition + "px")
							.style("top", yPosition + "px");

						if (d.tooltip) {
							scope.tooltip.html(d.tooltip);
						}
					})
					.on("mouseout", function () {
						scope.tooltip.transition().duration(500).style("opacity", 0);
					})
				;


			cell.append("svg:rect")
				.attr("width", function (d) {
					return Math.max(0, d.dx - 0.2);
				})
				.attr("height", function (d) {
					return Math.max(0, d.dy - 0.2);
				})
				.style("fill", function (d) {
					return d.children ? null : d.color;
				})
			;


			cell.append("svg:text")
				.attr("x", function (d) {
					return d.dx / 2;
				})
				.attr("y", function (d) {
					return d.dy / 2;
				})
				.attr("cursor", "default")
				.attr("text-anchor", "middle")
				.text(function (d) {
					return d.children ? null : d.label;
				});
			setTextStyles();

			function zoomed() {
				svg.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
				setTextStyles();
			}

			function setTextStyles() {
				svg.selectAll("text")
					.style('font-size', function (d) {
						var scale = d3.event && d3.event.scale ? d3.event.scale : 1;
						var size = Math.min(18 / scale, Math.floor(d.dx));
						return size + "px"
					})
					.attr("textLength", function (d) {
						return this.getComputedTextLength() < d.dx ? this.getComputedTextLength() : d.dx;
					})
					.style("opacity", function (d) {
						d.w = this.getComputedTextLength();
						return d.dx > d.w && d.showItem === true ? 1 : 0;
					})

				;
			}
		};


		PlotTreeMapNode.prototype.clear = function (scope) {
			if (!this.isRoot())
				return;
			scope.maing.selectAll("*").remove();
		};


		return PlotTreeMapNode;
	};
	beaker.bkoFactory('PlotTreeMapNode', ['plotUtils', retfunc]);
})();