We say that the action band says welcome when (
	There exists $p in $(#action-band) such that (
		$p's text matches "^Welcome.*"
	)
).

We say that we are in the login page when (
	There exists $div in $(.content-div) such that (
		( $div's class is ".sign-in" )
		And
		(Not ( $div's display is "none"))
	)
).

"""
  @name Login once
  @description We can't login when we are already logged in
  @severity Warning
"""
Always (
	If ( the action band says welcome ) Then (
		Not ( we are in the login page )
	)
).