/**
 * Access a default class from a different package
 */

package p{
	class DefaultClass{}
}

package q{
	import p.*;

	var defaultClass:DefaultClass;

}
