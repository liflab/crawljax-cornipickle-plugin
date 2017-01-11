We say that we are in the home page when (
	There exists $div in $(.content-div) such that (
		( $div's id is "home" )
		And
		(Not ( $div's display is "none"))
	)
).

We say that we are in the login page when (
	There exists $div in $(.content-div) such that (
		( $div's id is "sign-in" )
		And
		(Not ( $div's display is "none"))
	)
).

We say that the action band says welcome when (
	There exists $p in $(p#action-band) such that (
		$p's text matches "^Welcome.*"
	)
).

We say that the login is successful when (
	If (
		There exists $button in $(a.button-login) such that (
			$button's event is "click"
		)
	) Then (
		Next (
			( we are in the home page )
			And
			( the action band says welcome )
		)
	)
).

"""
  @name 
  @description All list items should either be left- or top-aligned.
  @severity Warning
"""
The next time ( the login is successful ) then (
	Always (
		Not (we are in the login page)
	)
).