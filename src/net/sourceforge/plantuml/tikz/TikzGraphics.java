/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2023, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * http://plantuml.com/patreon (only 1$ per month!)
 * http://plantuml.com/paypal
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 * 
 *
 */
package net.sourceforge.plantuml.tikz;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.Color;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.sourceforge.plantuml.Log;
import net.sourceforge.plantuml.Url;
import net.sourceforge.plantuml.eps.EpsGraphics;
import net.sourceforge.plantuml.ugraphic.UPath;
import net.sourceforge.plantuml.ugraphic.USegment;
import net.sourceforge.plantuml.ugraphic.USegmentType;
import net.sourceforge.plantuml.version.Version;

public class TikzGraphics {

	// https://www.sharelatex.com/blog/2013/08/27/tikz-series-pt1.html
	// http://cremeronline.com/LaTeX/minimaltikz.pdf

	private final List<String> cmd = new ArrayList<>();
	private final boolean withPreamble;

	private Color color = Color.BLACK;
	private Color fillcolor = Color.BLACK;
	private Color fillcolorGradient2 = null;
	private char gradientPolicy;
	private double thickness = 1.0;
	private final double scale;
	private String dash = null;

	private final Map<Color, String> colornames = new LinkedHashMap<Color, String>();

	public TikzGraphics(double scale, boolean withPreamble) {
		this.withPreamble = withPreamble;
		this.scale = scale;
	}

	private final Map<String, Integer> styles = new LinkedHashMap<String, Integer>();
	private final Map<String, String> stylesID = new HashMap<String, String>();

	private void addCommand(final StringBuilder sb) {
		final String s = sb.toString();
		cmd.add(s);
		if (s.startsWith("\\draw[") || s.startsWith("\\shade[")) {
			final int end = s.indexOf(']');
			if (end != -1) {
				final int start = s.indexOf('[');
				final String style = s.substring(start + 1, end);
				Integer count = styles.get(style);
				if (count == null) {
					count = 1;
					stylesID.put(style, "pstyle" + stylesID.size());
				} else {
					count++;
				}
				styles.put(style, count);
			}
		}
	}

	private String getColorName(Color c) {
		if (c.equals(Color.WHITE)) {
			return "white";
		}
		if (c.equals(Color.BLACK)) {
			return "black";
		}
		final String result = colornames.get(c);
		return Objects.requireNonNull(result);
	}

	public void createData(OutputStream os) throws IOException {
		if (withPreamble) {
			out(os, "\\documentclass{standalone}");
			out(os, "\\usepackage{tikz}");
			out(os, "\\usepackage{aeguill}");
			if (hasUrl) {
				out(os, "\\usetikzlibrary{calc}");
				out(os, "\\usepackage{hyperref}");
			}
			out(os, "\\begin{document}");
		}
		out(os, "% generated by Plantuml " + Version.versionString(15));
		if (hasUrl && withPreamble) {
			out(os, "\\tikzset{");
			out(os, "    href node/.style={");
			out(os, "        alias=sourcenode,");
			out(os, "        append after command={");
			out(os, "            let \\p1 = (sourcenode.north west),");
			out(os, "                \\p2=(sourcenode.south east),");
			out(os, "                \\n1={\\x2-\\x1},");
			out(os, "                \\n2={\\y2-\\y1} in");
			out(os, "            node [inner sep=0pt, outer sep=0pt,anchor=north west,at=(\\p1)] {\\href{#1}{\\XeTeXLinkBox{\\phantom{\\rule{\\n1}{\\n2}}}}}");
			out(os, "                    %xelatex needs \\XeTeXLinkBox, won't create a link unless it");
			out(os, "                    %finds text --- rules don't work without \\XeTeXLinkBox.");
			out(os, "                    %Still builds correctly with pdflatex and lualatex");
			out(os, "        }");
			out(os, "    }");
			out(os, "}");
			out(os, "\\tikzset{");
			out(os, "    hyperref node/.style={");
			out(os, "        alias=sourcenode,");
			out(os, "        append after command={");
			out(os, "            let \\p1 = (sourcenode.north west),");
			out(os, "                \\p2=(sourcenode.south east),");
			out(os, "                \\n1={\\x2-\\x1},");
			out(os, "                \\n2={\\y2-\\y1} in");
			out(os, "            node [inner sep=0pt, outer sep=0pt,anchor=north west,at=(\\p1)] {\\hyperref [#1]{\\XeTeXLinkBox{\\phantom{\\rule{\\n1}{\\n2}}}}}");
			out(os, "                    %xelatex needs \\XeTeXLinkBox, won't create a link unless it");
			out(os, "                    %finds text --- rules don't work without \\XeTeXLinkBox.");
			out(os, "                    %Still builds correctly with pdflatex and lualatex");
			out(os, "        }");
			out(os, "    }");
			out(os, "}");
		}
		for (Map.Entry<Color, String> ent : colornames.entrySet()) {
			out(os, definecolor(ent.getValue(), ent.getKey()));

		}
		if (scale != 1) {
			out(os, "\\scalebox{" + format(scale) + "}{");
		}
		out(os, "\\begin{tikzpicture}[yscale=-1");
		purgeStyles();
		for (String style : styles.keySet()) {
			out(os, "," + stylesID.get(style) + "/.style={" + style + "}");
		}
		out(os, "]");
		for (String s : cmd) {
			out(os, useStyle(s));
		}
		out(os, "\\end{tikzpicture}");
		if (scale != 1) {
			out(os, "}");
		}
		if (withPreamble) {
			out(os, "\\end{document}");
		}
	}

	private String useStyle(String s) {
		for (String style : styles.keySet()) {
			final String start1 = "\\draw[" + style + "]";
			if (s.startsWith(start1)) {
				final String newStart = "\\draw[" + stylesID.get(style) + "]";
				return newStart + s.substring(start1.length());
			}
			final String start2 = "\\shade[" + style + "]";
			if (s.startsWith(start2)) {
				final String newStart = "\\shade[" + stylesID.get(style) + "]";
				return newStart + s.substring(start2.length());
			}
		}
		return s;
	}

	private void purgeStyles() {
		for (Iterator<Map.Entry<String, Integer>> it = styles.entrySet().iterator(); it.hasNext();) {
			final Map.Entry<String, Integer> ent = it.next();
			if (ent.getValue().intValue() == 1) {
				it.remove();
			}
		}
	}

	private String definecolor(String name, Color color) {
		return "\\definecolor{" + name + "}{RGB}{" + color.getRed() + "," + color.getGreen() + "," + color.getBlue()
				+ "}";
	}

	public void rectangle(double x, double y, double width, double height) {
		final StringBuilder sb = new StringBuilder();
		if (pendingUrl == null) {
			appendShadeOrDraw(sb);
			sb.append("line width=" + thickness + "pt] ");
			sb.append(couple(x, y) + " rectangle " + couple(x + width, y + height));
			sb.append(";");
		} else {
			sb.append("\\node at " + couple(x, y) + "[draw, ");
			if (color != null) {
				sb.append("color=" + getColorName(color) + ",");
			}
			if (fillcolor != null) {
				sb.append("fill=" + getColorName(fillcolor) + ",");
				if (color == null) {
					sb.append("color=" + getColorName(fillcolor) + ",");
				}
			}
			sb.append("line width=" + thickness + "pt,");
			sb.append("below right,");
			sb.append("inner sep=2ex,");
			sb.append("minimum width=" + format(width) + "pt,");
			sb.append("minimum height=" + format(height) + "pt,");
			if (Url.isLatex(pendingUrl)) {
				sb.append("hyperref node=" + extractInternalHref(pendingUrl));
			} else {
				sb.append("href node=" + pendingUrl);
			}
			sb.append("] ");
			sb.append(" {};");
			urlIgnoreText = true;
		}
		addCommand(sb);
	}

	private String getAngleFromGradientPolicy() {
		if (this.gradientPolicy == '-') {
			return "0";
		}
		if (this.gradientPolicy == '|') {
			return "90";
		}
		if (this.gradientPolicy == '/') {
			return "45";
		}
		if (this.gradientPolicy == '\\') {
			return "135";
		}
		throw new IllegalArgumentException();
	}

	private String couple(double x, double y) {
		return "(" + format(x) + "pt," + format(y) + "pt)";
	}

	public static String format(double x) {
		return EpsGraphics.formatSimple4(x);
	}

	private void out(OutputStream os, String s) throws IOException {
		os.write(s.getBytes(UTF_8));
		os.write("\n".getBytes(UTF_8));
	}

	public void text(double x, double y, String text, boolean underline, boolean italic, boolean bold) {
		final StringBuilder sb = new StringBuilder("\\node at " + couple(x, y));
		sb.append("[below right");
		if (color != null) {
			sb.append(",color=");
			sb.append(getColorName(color));
		}
		sb.append("]{");
		if (pendingUrl == null || urlIgnoreText) {
			if (underline) {
				sb.append("\\underline{");
			}
			if (italic) {
				sb.append("\\textit{");
			}
			if (bold) {
				sb.append("\\textbf{");
			}
			sb.append(protectText(text));
			if (bold) {
				sb.append("}");
			}
			if (italic) {
				sb.append("}");
			}
			if (underline) {
				sb.append("}");
			}
		} else {
			appendPendingUrl(sb);
			sb.append("{");
			sb.append(protectText(text));
			sb.append("}");
		}
		sb.append("};");
		addCommand(sb);
	}

	public void appendRaw(double x, double y, String formula) {
		Objects.requireNonNull(formula);
		final StringBuilder sb = new StringBuilder("\\node at " + couple(x, y));
		sb.append("[below right");
		sb.append("]{");
		sb.append("{");
		sb.append(formula);
		sb.append("}");
		sb.append("};");
		addCommand(sb);
	}

	private void appendPendingUrl(final StringBuilder sb) {
		if (Url.isLatex(pendingUrl)) {
			sb.append("\\hyperref[");
			sb.append(extractInternalHref(pendingUrl));
			sb.append("]");
		} else {
			sb.append("\\href{");
			sb.append(pendingUrl);
			sb.append("}");
		}
	}

	private static String extractInternalHref(String pendingUrl) {
		if (Url.isLatex(pendingUrl) == false) {
			throw new IllegalArgumentException();
		}
		return pendingUrl.substring("latex://".length());
	}

	private String protectText(String text) {
		text = text.replaceAll("\\\\", "\\\\\\\\");
		text = text.replaceAll("_", "\\\\_");
		text = text.replaceAll("\u00AB", "\\\\guillemotleft ");
		text = text.replaceAll("\u00BB", "\\\\guillemotright ");
		text = text.replaceAll("<", "\\\\textless ");
		text = text.replaceAll(">", "\\\\textgreater ");
		text = text.replaceAll("&", "\\\\&");
		text = text.replaceAll("%", "\\\\%");
		text = text.replace("$", "\\$");
		text = text.replace("{", "\\{");
		text = text.replace("}", "\\}");
		// text = text.replaceAll("~", "\\\\~{}");
		text = text.replace("~", "{\\raise.35ex\\hbox{$\\scriptstyle\\mathtt{\\sim}$}}");
		// {\raise.35ex\hbox{$\scriptstyle\mathtt{\sim}$}}
		// {\\raise.35ex\\hbox{$\\scriptstyle\\mathtt{\\sim}$}}
		return text;
	}

	public void line(double x1, double y1, double x2, double y2) {
		final StringBuilder sb = new StringBuilder();
		sb.append("\\draw[");
		if (color != null) {
			sb.append("color=" + getColorName(color) + ",");
		}
		sb.append("line width=" + thickness + "pt");
		if (dash != null) {
			sb.append(",dash pattern=" + dash);
		}
		sb.append("] ");
		sb.append(couple(x1, y1));
		sb.append(" -- ");
		sb.append(couple(x2, y2));
		sb.append(";");
		addCommand(sb);
	}

	public void polygon(double[] points) {

		assert points.length % 2 == 0;
		final StringBuilder sb = new StringBuilder();
		appendShadeOrDraw(sb);
		sb.append("line width=" + thickness + "pt]");
		sb.append(" ");
		for (int i = 0; i < points.length; i += 2) {
			sb.append(couple(points[i], points[i + 1]));
			sb.append(" -- ");
		}
		sb.append("cycle;");
		addCommand(sb);
	}

	private void round(double r, double[] points) {
		assert points.length % 2 == 0;
		final StringBuilder sb = new StringBuilder();
		appendShadeOrDraw(sb);
		sb.append("line width=" + thickness + "pt]");
		sb.append(" ");
		int i = 0;
		sb.append(couple(points[i++], points[i++]));
		sb.append(" arc (180:270:" + format(r) + "pt) -- ");
		sb.append(couple(points[i++], points[i++]));
		sb.append(" -- ");
		sb.append(couple(points[i++], points[i++]));
		sb.append(" arc (270:360:" + format(r) + "pt) -- ");
		sb.append(couple(points[i++], points[i++]));
		sb.append(" -- ");
		sb.append(couple(points[i++], points[i++]));
		sb.append(" arc (0:90:" + format(r) + "pt) -- ");
		sb.append(couple(points[i++], points[i++]));
		sb.append(" -- ");
		sb.append(couple(points[i++], points[i++]));
		sb.append(" arc (90:180:" + format(r) + "pt) -- ");
		sb.append(couple(points[i++], points[i++]));
		sb.append(" -- ");
		sb.append("cycle;");
		addCommand(sb);
	}

	private void appendShadeOrDraw(final StringBuilder sb) {
		final boolean gradient = this.fillcolorGradient2 != null;
		sb.append(gradient ? "\\shade[" : "\\draw[");
		if (color != null) {
			sb.append(gradient ? "draw=" : "color=");
			sb.append(getColorName(color) + ",");
		}
		if (gradient) {
			sb.append("top color=" + getColorName(fillcolor) + ",");
			sb.append("bottom color=" + getColorName(fillcolorGradient2) + ",");
			sb.append("shading=axis,shading angle=" + getAngleFromGradientPolicy() + ",");
		} else if (fillcolor != null) {
			sb.append("fill=" + getColorName(fillcolor) + ",");
			if (color == null) {
				sb.append("color=" + getColorName(fillcolor) + ",");
			}
		}
	}

	public void rectangleRound(double x, double y, double width, double height, double r) {
		double[] points = new double[8 * 2];
		points[0] = x;
		points[1] = y + r;
		points[2] = x + r;
		points[3] = y;

		points[4] = x + width - r;
		points[5] = y;
		points[6] = x + width;
		points[7] = y + r;

		points[8] = x + width;
		points[9] = y + height - r;
		points[10] = x + width - r;
		points[11] = y + height;

		points[12] = x + r;
		points[13] = y + height;
		points[14] = x;
		points[15] = y + height - r;

		round(r, points);
	}

	public void upath(double x, double y, UPath path) {
		final StringBuilder sb = new StringBuilder();
		appendShadeOrDraw(sb);
		sb.append("line width=" + thickness + "pt");
		if (dash != null) {
			sb.append(",dash pattern=" + dash);
		}
		sb.append("] ");
		for (USegment seg : path) {
			final USegmentType type = seg.getSegmentType();
			final double coord[] = seg.getCoord();
			if (type == USegmentType.SEG_MOVETO) {
				sb.append(couple(coord[0] + x, coord[1] + y));
			} else if (type == USegmentType.SEG_LINETO) {
				sb.append(" -- ");
				sb.append(couple(coord[0] + x, coord[1] + y));
			} else if (type == USegmentType.SEG_QUADTO) {
				throw new UnsupportedOperationException();
			} else if (type == USegmentType.SEG_CUBICTO) {
				// curvetoNoMacro(coord[0] + x, coord[1] + y, coord[2] + x, coord[3] + y,
				// coord[4] + x, coord[5] + y);
				sb.append(" ..controls ");
				sb.append(couple(coord[0] + x, coord[1] + y));
				sb.append(" and ");
				sb.append(couple(coord[2] + x, coord[3] + y));
				sb.append(" .. ");
				sb.append(couple(coord[4] + x, coord[5] + y));
			} else if (type == USegmentType.SEG_CLOSE) {
				// Nothing
			} else if (type == USegmentType.SEG_ARCTO) {
				// Nothing
			} else {
				Log.println("unknown4 " + seg);
			}
		}
		sb.append(";");
		addCommand(sb);
	}

	public void ellipse(double x, double y, double width, double height) {
		final StringBuilder sb = new StringBuilder();
		sb.append("\\draw[");
		if (color != null) {
			sb.append("color=" + getColorName(color) + ",");
		}
		if (fillcolor != null) {
			sb.append("fill=" + getColorName(fillcolor) + ",");
		}
		sb.append("line width=" + thickness + "pt] " + couple(x, y) + " ellipse (" + format(width) + "pt and "
				+ format(height) + "pt);");
		addCommand(sb);
	}

	public void arc(double x, double y, int angleStart, int angleEnd, double radius) {
		final StringBuilder sb = new StringBuilder();
		sb.append("\\draw[");
		if (color != null) {
			sb.append("color=" + getColorName(color) + ",");
		}
		if (fillcolor != null) {
			sb.append("fill=" + getColorName(fillcolor) + ",");
		}
		sb.append("line width=" + thickness + "pt] " + couple(x, y) + " arc (" + angleStart + ":" + angleEnd + ":"
				+ format(radius) + "pt);");
		addCommand(sb);
	}

	public void drawSingleCharacter(double x, double y, char c) {
		final StringBuilder sb = new StringBuilder();
		sb.append("\\node at ");
		sb.append(couple(x, y));
		sb.append("[]{\\textbf{\\Large " + c + "}};");
		addCommand(sb);
	}

	public void drawPathIterator(double x, double y, PathIterator path) {
		final StringBuilder sb = new StringBuilder(
				"\\draw[color=" + getColorName(color) + ",fill=" + getColorName(color) + "] ");
		final double coord[] = new double[6];
		while (path.isDone() == false) {
			final int code = path.currentSegment(coord);
			if (code == PathIterator.SEG_MOVETO) {
				sb.append(couple(coord[0] + x, coord[1] + y));
			} else if (code == PathIterator.SEG_LINETO) {
				sb.append(" -- ");
				sb.append(couple(coord[0] + x, coord[1] + y));
			} else if (code == PathIterator.SEG_CLOSE) {
				sb.append(";");
				addCommand(sb);
				sb.setLength(0);
				sb.append("\\draw ");
			} else if (code == PathIterator.SEG_CUBICTO) {
				sb.append(" ..controls ");
				sb.append(couple(coord[0] + x, coord[1] + y));
				sb.append(" and ");
				sb.append(couple(coord[2] + x, coord[3] + y));
				sb.append(" .. ");
				sb.append(couple(coord[4] + x, coord[5] + y));
			} else if (code == PathIterator.SEG_QUADTO) {
				sb.append(" ..controls ");
				sb.append(couple(coord[0] + x, coord[1] + y));
				sb.append(" .. ");
				sb.append(couple(coord[2] + x, coord[3] + y));
			} else {
				throw new UnsupportedOperationException("code=" + code);
			}

			path.next();
		}

		// eps.fill(path.getWindingRule());

	}

	public void setFillColor(Color c) {
		// if (c == null) {
		// c = Color.WHITE;
		// }
		this.fillcolor = c;
		this.fillcolorGradient2 = null;
		addColor(c);
	}

	public void setGradientColor(Color c1, Color c2, char policy) {
		this.fillcolor = c1;
		this.fillcolorGradient2 = c2;
		this.gradientPolicy = policy;
		addColor(c1);
		addColor(c2);
	}

	public void setStrokeColor(Color c) {
		// Objects.requireNonNull(c);
		this.color = c;
		addColor(c);
	}

	private void addColor(Color c) {
		if (c == null) {
			return;
		}
		if (colornames.containsKey(c)) {
			return;
		}
		final String name = "plantucolor" + String.format("%04d", colornames.size());
		colornames.put(c, name);
	}

	public void setStrokeWidth(double thickness, String dash) {
		this.thickness = thickness;
		this.dash = dash;
	}

	private String pendingUrl = null;
	private boolean urlIgnoreText = false;
	private boolean hasUrl = false;

	public void openLink(String url, String title) {
		this.hasUrl = true;
		this.urlIgnoreText = false;
		this.pendingUrl = Objects.requireNonNull(url);
		//
		// if (pendingLink2.size() > 0) {
		// closeLink();
		// }
		//
		// pendingLink2.add(0, (Element) document.createElement("a"));
		// pendingLink2.get(0).setAttribute("target", target);
		// pendingLink2.get(0).setAttribute("xlink:href", url);
		// if (title == null) {
		// pendingLink2.get(0).setAttribute("xlink:title", url);
		// } else {
		// pendingLink2.get(0).setAttribute("xlink:title", title);
		// }
	}

	public void closeLink() {
		this.pendingUrl = null;
	}

}
