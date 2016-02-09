Rules for creating csv maps:

* Values must be comma separated
--- COLUMN NAMES ---
* 1st column has a name of the facet for which we are creating normalisation vocabulary
* 2nd column has a name COUNT and it is optional
* 3rd column has a name NORMALIZED_VALUE
* next columns are reserved for decomposition. For each decomposed value create new column.
* last column is ignored and it is reserved for comments
* column names that contains facet names (1st and decomposition columns) must match solr index names, here is the list 
 (for more details see schema.xml in vlo-solr project:
	iD, COLLECTION, NAME, PROJECTNAME, CONTINENT, COUNTRY, LANGUAGECODE, AVAILABILITY, LICENSE, ORGANISATION, GENRE, MODALITY,
	SUBJECT, DESCRIPTION, RESOURCECLASS,FORMAT, DATAPROVIDER, NATIONALPROJECT, KEYWORDS, TEMPORALCOVERAGE, LIFECYCLESTATUS,
	DISTRIBUTIONTYPE, RIGHTSHOLDER, TEXT
	
--- NORMALIZED_VALUE ---
* NORMALIZED_VALUE value must exist
* If you want to map some value to more then one NORMALIZED_VALUE use semicolon as separator
* If NORMALIZED_VALUE is quoted, quotes will be ignored
* If NORMALIZED_VALUE is equal to --, original value will not be shown in VLO UI 


* If you want to skip any row put C style one line comments: // at the beginning of the line
Do this for rows that are not finished to avoid exceptions or to ignore mapping


* 

EXAMPLES
remark: to avoid confusion instead of separating values with tabs we will surround them with <> like <a><b><c>

RESOURCECLASS	NORMALIZED_VALUE	GENRE	SUBJECT	Remarks

<foo><text><history><><some remarks> -> foo will be normalized with text and facet GENRE will get the value history

<bar><text><><love><some other remarks> -> bar will be normalized with text and facet SUBJECT will get the value love

<baz><"text"><><><> -> baz will be normalized with text, not with "text"

<qux><text; corpus><><><> -> qux will be searchable under two facet values: text and corpus

<quux><><><><> -> this line is invalid, NORMALIZED_VALUE is missing

//<corge><><><><> -> this line is commented and will be ignored