/**
 * An array of DOM attributes to include. Results in a smaller
 * JSON by reporting only attributes that appear in the properties
 * to evaluate.
 */
var m_attributesToInclude = [%%ATTRIBUTELIST%%];

/**
 * An array of tag names attributes to include. Results in a smaller
 * JSON by reporting only tags that appear in the properties
 * to evaluate.
 */
var m_tagsToInclude = [%%TAGLIST%%];

var INCLUDE = 0;
var DONT_INCLUDE = 1;
var DONT_INCLUDE_RECURSIVE = 2;
/**
 * A global counter to give a unique ID to every element
 * encountered and reported back to the server
 */
var elementCounter = 0;

var m_idMap = {};

var remove_units = function(s)
{
	if (typeof s == "string" || s instanceof String)
	{
		s = s.replace("px", "");
	}
	return Number(s);

};

/**
 * Gets the class list of the element using className
 * and classList DOM attributes combined, to make sure
 * we get the class names in every case
 * @param  element  The DOM element to retrieve its classes
 * @return A list of class names separated by spaces
 */
var get_class_list = function(element)
{
	var out = "";
	if (element.className)
	{
		out += element.className; //className already is a space separated class name string
	}
	if (element.classList) //supported by IE9+ only; classList is an array of class names
	{
		for(var i = 0; i < element.classList.length; i++)
		{
			if(out.indexOf(element.classList[i]) === -1)
			{
				out += " ";
				out += element.classList[i];
			}
		}
	}
	return out;
};

/**
 * Computes the absolute coordinates of an element
 * with respect to the document
 * @param element The element to get the position
 * @return A JSON structure giving the cumulative top and left
 *   properties
 */
var cumulativeOffset = function(element)
{
	var top = 0, left = 0;
	do
	{
		top += element.offsetTop  || 0;
		left += element.offsetLeft || 0;
		element = element.offsetParent;
	} while(element);

	return {
		top: top,
		left: left
	};
};

/**
 * Checks if an array contains an element
 * @param a The array
 * @param obj The element
 * @return True or false
 */
var array_contains = function(a, obj)
{
	for (var i = 0; i < a.length; i++)
	{
		if (a[i] === obj)
		{
			return true;
		}
	}
	return false;
};

/**
 * Checks if an object is empty
 */
var is_empty = function(object)
{
	for (var i in object)
	{
		return false;
	}
	return true;
};

var escape_json_string = function(key, value)
{
	if (typeof value === "string" || value instanceof String)
	{
		// Escape some characters left of by encodeURI
		value = value.replace(/&/g, "%26");
		value = value.replace(/=/g, "%3D");
	}
	return value;
};

var add_dimensions = function(dimensions)
{
	var sum = 0;
	for (var i = 0; i < dimensions.length; i++)
	{
		var d = dimensions[i];
		sum += remove_units(d);
	}
	return sum;
};

// http://stackoverflow.com/a/9824480
var get_orientation = function()
{
	switch(window.orientation) 
    {  
      case -90:
      case 90:
        return "landscape";
      default:
        return "portrait";
    }
};

var setValue = function(elem)
{
	//value property is only defined for input elements
	if (elem.tagName === "INPUT" || elem.tagName === "BUTTON")
	{
		if (elem.type === "range" || elem.type == "number")
		{
			return elem.valueAsNumber;
		}
		else
		{
			return elem.value;
		}
	}
};

/* Obtained from http://stackoverflow.com/a/25078870 */
var getStyle = function(elem, prop)
{
	var res = null;
	if (elem.currentStyle)
	{
		res = elem.currentStyle[prop];
	}
	else if (window.getComputedStyle)
	{
		if (window.getComputedStyle.getPropertyValue)
		{
			res = window.getComputedStyle(elem, null).getPropertyValue(prop);
		}
		else
		{
			res = window.getComputedStyle(elem)[prop];
		}
	}
	return res;
};

/**
 * Creates a style string for an element's border.
 * Caveat emptor: only reads the properties for the top border!
 */
var formatBorderString = function(elem)
{
	var s_top_style = getStyle(elem, "border-top-style");
	var s_top_colour = getStyle(elem, "border-top-color");
	var s_top_width = getStyle(elem, "border-top-width");
	var out = s_top_style + " " + s_top_colour + " " + s_top_width;
	return out.trim();
};

var formatBackgroundString = function(elem)
{
	var s_background_color = getStyle(elem, "background-color");
	return s_background_color.trim();
};

var formatBool = function(property)
{
	if (property) {return "true";}
	else {return "false";}
};

var registerNewElement = function(n)
{
	if (n.cornipickleid !== undefined)
	{
		return;
	}
	n.cornipickleid = elementCounter;
	m_idMap[elementCounter] = {
		"element" : n,
		"style" : {}
	};
	elementCounter++;
};


var addIfDefined = function(out, property_name, property)
{
	// First check if this attribute must be included in the report
	if (array_contains(m_attributesToInclude, property_name))
	{
		// Yes, now check if it is defined
		if (property !== undefined && property !== "")
		{
			out[property_name] = property;
		}
	}
	return out;
};

/**
 * Checks whether an element's tag, class and ID name match the
 * CSS selector element.
 */
var matchesSelector = function(selector, n)
{
	var pat = new RegExp("([\\w\\d]+){0,1}(\\.([\\w\\d]+)){0,1}(#([\\w\\d]+)){0,1}");
	var mat = pat.exec(selector);
	var tag_name = mat[1];
	var class_name = mat[3];
	var id_name = mat[5];
	if (tag_name !== undefined)
	{
		if (!n.tagName || n.tagName.toLowerCase() !== tag_name.toLowerCase())
		{
			return false;
		}
	}
	if (class_name !== undefined)
	{
		if (!n.className)
		{
			return false;
		}
		var class_parts = get_class_list(n).split(" "); //n.className.split(" ");
		if (!array_contains(class_parts, class_name))
		{
			return false;
		}
	}
	if (id_name !== undefined)
	{
		if (!n.id || n.id !== id_name)
		{
			return false;
		}
	}
	return true;
};

var includeInResult = function(n, path)
{
	var classlist = get_class_list(n);
	if (classlist)
	{
		if (classlist.indexOf("nocornipickle") !== -1) // This is the probe itself
		{
			return DONT_INCLUDE_RECURSIVE;
		}
	}
	if (!n.tagName) // This is a text node
	{
		if (n.nodeValue.trim() === "")
		{
			// Don't include nodes containing only whitespace
			return DONT_INCLUDE_RECURSIVE;
		}
		else
		{
			return INCLUDE;
		}
	}
	for (var i = 0; i < m_tagsToInclude.length; i++)
	{
		var part = m_tagsToInclude[i];
		if (matchesSelector(part, n))
		{
			return INCLUDE;
		}
	}
	return DONT_INCLUDE;
};

var serializeMediaQueries = function()
{
	var out = {};
	for (var i = 0; i < m_attributesToInclude.length; i++)
	{
		var att = m_attributesToInclude[i];
		var indexOfUnderscore = att.indexOf("_");
		var query = "";
		var id = -1;
		if(indexOfUnderscore !== -1)
		{
			id = att.substring(0,indexOfUnderscore);
			query = att.substring(indexOfUnderscore + 1, att.length);
			if(window.matchMedia(query).matches)
			{
				out[id] = "true";
			}
			else
			{
				out[id] = "false";
			}
		}
	}
	return out;
}

var serializeWindow = function(page_contents)
{
	return {
		"tagname" : "window",
		"URL" : window.location.host + window.location.pathname,
		"aspect-ratio" : window.document.documentElement.clientWidth / window.document.documentElement.clientHeight,
		"orientation" : get_orientation(),
		"width" : window.document.documentElement.clientWidth,
		"height" : window.document.documentElement.clientHeight,
		"device-width" : window.screen.availWidth,
		"device-height" : window.screen.availHeight,
		"device-aspect-ratio" : window.screen.availWidth / window.screen.availHeight,
		"mediaqueries" : serializeMediaQueries(),
		"children" : [ page_contents ]
	};
};

/**
 * Serializes the contents of the page. This method recursively
 * traverses the DOM and produces a JSON structure of some of
 * its elements' properties
 * @param n The DOM node to analyze
 * @param path The path from the root of the DOM, expressed as
 *   an array of tag names
 * @param force_inclusion Set to true to force the inclusion of
 *   n in the result, irrelevant of whether the node should be
 *   included according to the normal criteria
 */
var serializePageContents = function(n, path, event)
{
	var current_path = path;
	current_path.push(n.tagName);
	var out = {};
	if (includeInResult(n, path) === INCLUDE || (event && n === event))
	{
		if (n.tagName)
		{
			// Gives the element a unique ID, if it doesn't have one
			registerNewElement(n);
			var pos = cumulativeOffset(n);
			out.tagname = n.tagName.toLowerCase();
			out.cornipickleid = n.cornipickleid;
			out = addIfDefined(out, "value", setValue(n));
			out = addIfDefined(out, "class", n.className);
			out = addIfDefined(out, "id", n.id);
			out = addIfDefined(out, "height", n.clientHeight);
			out = addIfDefined(out, "width", n.clientWidth);
			out = addIfDefined(out, "background", formatBackgroundString(n));
			out = addIfDefined(out, "color", getStyle(n, "color"));
			out = addIfDefined(out, "border", formatBorderString(n));
			out = addIfDefined(out, "top", pos.top);
			out = addIfDefined(out, "left", pos.left);
			out = addIfDefined(out, "bottom", add_dimensions([pos.top, n.clientHeight]));
			out = addIfDefined(out, "right", add_dimensions([pos.left,  n.clientWidth]));
			out = addIfDefined(out, "display", getStyle(n, "display"));
			out = addIfDefined(out, "size", n.size);
			out = addIfDefined(out, "checked", formatBool(n.checked));
			out = addIfDefined(out, "disabled", formatBool(n.disabled));
			out = addIfDefined(out, "accesskey", n.accessKey);
			out = addIfDefined(out, "min", n.min);
			if (event && n === event)
			{
				out.event = "click";
			}
			if (n.tagName === "INPUT")
			{
				// Form fields require special treatment
				if (n.type === "text") // Textbox
				{
					// We create a single text child with the box's contents
					out.children = [{
						"tagname" : "CDATA",
						"text" : n.value
					}];
					return out; // No need to recurse
				}
			}
			else if (n.tagName === "BUTTON")
			{
				// We create a single text child with the button's text
				out.children = [{
					"tagname" : "CDATA",
					"text" : n.innerHTML
				}];
				return out; // No need to recurse
			}
		}
		else
		{
			out.tagname = "CDATA";
			out.text = n.nodeValue;
			return out;
		}
	}
	if (includeInResult(n, path) !== DONT_INCLUDE_RECURSIVE)
	{
		var in_children = [];
		for (var i = 0; i < n.childNodes.length; i++)
		{
			var child = n.childNodes[i];
			var new_child = serializePageContents(child, current_path, event);
			if (!is_empty(new_child))
			{
				in_children.push(new_child);
			}
		}
		if (in_children.length > 0)
		{
			out.children = in_children;
		}
	}
	return out;
};

var target;
if(%%BOOL%%)
{
	console.log("I'm in");
	target = document.evaluate("%%PATH%%", document, null, 9, null).singleNodeValue;
	console.log(target);
}
else
{
	target = undefined;
}

var json = serializePageContents(document.body, [], target);
return JSON.stringify(serializeWindow(json));