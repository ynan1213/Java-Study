/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

grammar BaseRule;

import Symbol, Keyword, SQL92Keyword, Literals;

parameterMarker
    : QUESTION_
    ;

literals
    : stringLiterals
    | numberLiterals
    | dateTimeLiterals
    | hexadecimalLiterals
    | bitValueLiterals
    | booleanLiterals
    | nullValueLiterals
    ;

stringLiterals
    : characterSetName_? STRING_ collateClause_?
    ;

numberLiterals
   : MINUS_? NUMBER_
   ;

dateTimeLiterals
    : (DATE | TIME | TIMESTAMP) STRING_
    | LBE_ identifier_ STRING_ RBE_
    ;

hexadecimalLiterals
    : characterSetName_? HEX_DIGIT_ collateClause_?
    ;

bitValueLiterals
    : characterSetName_? BIT_NUM_ collateClause_?
    ;
    
booleanLiterals
    : TRUE | FALSE
    ;

nullValueLiterals
    : NULL
    ;

identifier_
    : IDENTIFIER_ | unreservedWord_
    ;

variable_
    : (AT_? AT_)? (GLOBAL | LOCAL)? DOT_? identifier_
    ;

unreservedWord_
    : ADA
    | C92 | CATALOG_NAME | CHARACTER_SET_CATALOG | CHARACTER_SET_NAME | CHARACTER_SET_SCHEMA
    | CLASS_ORIGIN | COBOL | COLLATION_CATALOG | COLLATION_NAME | COLLATION_SCHEMA
    | COLUMN_NAME | COMMAND_FUNCTION | COMMITTED | CONDITION_NUMBER | CONNECTION_NAME
    | CONSTRAINT_CATALOG | CONSTRAINT_NAME | CONSTRAINT_SCHEMA | CURSOR_NAME
    | DATA | DATETIME_INTERVAL_CODE | DATETIME_INTERVAL_PRECISION | DYNAMIC_FUNCTION
    | FORTRAN
    | LENGTH
    | MESSAGE_LENGTH | MESSAGE_OCTET_LENGTH | MESSAGE_TEXT | MORE92 | MUMPS
    | NAME | NULLABLE | NUMBER
    | PASCAL | PLI
    | REPEATABLE | RETURNED_LENGTH | RETURNED_OCTET_LENGTH | RETURNED_SQLSTATE | ROW_COUNT
    | SCALE | SCHEMA_NAME | SERIALIZABLE | SERVER_NAME | SUBCLASS_ORIGIN
    | TABLE_NAME | TYPE
    | UNCOMMITTED | UNNAMED
    ;

schemaName
    : identifier_
    ;

tableName
    : (owner DOT_)? name
    ;

columnName
    : (owner DOT_)? name
    ;

viewName
    : identifier_
    | (owner DOT_)? identifier_
    ;

owner
    : identifier_
    ;

name
    : identifier_
    ;

columnNames
    : LP_? columnName (COMMA_ columnName)* RP_?
    ;

tableNames
    : LP_? tableName (COMMA_ tableName)* RP_?
    ;

characterSetName_
    : IDENTIFIER_
    ;

expr
    : expr logicalOperator expr
    | notOperator_ expr
    | LP_ expr RP_
    | booleanPrimary_
    ;

logicalOperator
    : OR | AND | AND_
    ;

notOperator_
    : NOT | NOT_
    ;

booleanPrimary_
    : booleanPrimary_ IS NOT? (TRUE | FALSE | UNKNOWN | NULL)
    | booleanPrimary_ SAFE_EQ_ predicate
    | booleanPrimary_ comparisonOperator predicate
    | booleanPrimary_ comparisonOperator (ALL | ANY) subquery
    | predicate
    ;

comparisonOperator
    : EQ_ | GTE_ | GT_ | LTE_ | LT_ | NEQ_
    ;

predicate
    : bitExpr NOT? IN subquery
    | bitExpr NOT? IN LP_ expr (COMMA_ expr)* RP_
    | bitExpr NOT? BETWEEN bitExpr AND predicate
    | bitExpr NOT? LIKE simpleExpr (ESCAPE simpleExpr)?
    | bitExpr
    ;

bitExpr
    : bitExpr VERTICAL_BAR_ bitExpr
    | bitExpr AMPERSAND_ bitExpr
    | bitExpr SIGNED_LEFT_SHIFT_ bitExpr
    | bitExpr SIGNED_RIGHT_SHIFT_ bitExpr
    | bitExpr PLUS_ bitExpr
    | bitExpr MINUS_ bitExpr
    | bitExpr ASTERISK_ bitExpr
    | bitExpr SLASH_ bitExpr
    | bitExpr MOD_ bitExpr
    | bitExpr CARET_ bitExpr
    | bitExpr PLUS_ intervalExpression_
    | bitExpr MINUS_ intervalExpression_
    | simpleExpr
    ;

simpleExpr
    : functionCall
    | parameterMarker
    | literals
    | columnName
    | simpleExpr COLLATE (STRING_ | identifier_)
    | variable_
    | (PLUS_ | MINUS_ | TILDE_ | NOT_) simpleExpr
    | LP_ expr (COMMA_ expr)* RP_
    | EXISTS? subquery
    | LBE_ identifier_ expr RBE_
    | matchExpression_
    | caseExpression_
    | intervalExpression_
    ;

functionCall
    : aggregationFunction | specialFunction_ | regularFunction_ 
    ;

aggregationFunction
    : aggregationFunctionName_ LP_ distinct? (expr (COMMA_ expr)* | ASTERISK_)? RP_
    ;

aggregationFunctionName_
    : MAX | MIN | SUM | COUNT | AVG
    ;

distinct
    : DISTINCT
    ;

specialFunction_
    : castFunction_ | convertFunction_ | positionFunction_ | substringFunction_ | extractFunction_ | trimFunction_
    ;

castFunction_
    : CAST LP_ (expr | NULL) AS dataType RP_
    ;

convertFunction_
    : CONVERT LP_ expr USING identifier_ RP_
    ;

positionFunction_
    : POSITION LP_ expr IN expr RP_
    ;

substringFunction_
    : SUBSTRING LP_ expr FROM NUMBER_ (FOR NUMBER_)? RP_
    ;

extractFunction_
    : EXTRACT LP_ identifier_ FROM expr RP_
    ;

trimFunction_
    : TRIM LP_ (LEADING | BOTH | TRAILING) STRING_ FROM STRING_ RP_
    ;

regularFunction_
    : regularFunctionName_ LP_ (expr (COMMA_ expr)* | ASTERISK_)? RP_
    ;

regularFunctionName_
    : identifier_ | IF | CURRENT_TIMESTAMP | LOCALTIME | LOCALTIMESTAMP | INTERVAL
    ;

matchExpression_
    : literals MATCH UNIQUE? (PARTIAL | FULL)  subquery
    ;

caseExpression_
    : CASE simpleExpr? caseWhen_+ caseElse_? END
    ;

caseWhen_
    : WHEN expr THEN expr
    ;

caseElse_
    : ELSE expr
    ;

intervalExpression_
    : INTERVAL expr intervalUnit_
    ;

intervalUnit_
    : MICROSECOND | SECOND | MINUTE | HOUR | DAY | WEEK | MONTH | QUARTER | YEAR
    ;

subquery
    : 'Default does not match anything'
    ;

orderByClause
    : ORDER BY orderByItem (COMMA_ orderByItem)*
    ;

orderByItem
    : (columnName | numberLiterals) (ASC | DESC)?
    ;

dataType
    : dataTypeName_ dataTypeLength? characterSet_? collateClause_? | dataTypeName_ LP_ STRING_ (COMMA_ STRING_)* RP_ characterSet_? collateClause_?
    ;

dataTypeName_
    : identifier_ identifier_?
    ;

dataTypeLength
    : LP_ NUMBER_ (COMMA_ NUMBER_)? RP_
    ;

characterSet_
    : (CHARACTER | CHAR) SET EQ_? ignoredIdentifier_
    ;

collateClause_
    : COLLATE EQ_? (STRING_ | ignoredIdentifier_)
    ;

ignoredIdentifier_
    : identifier_ (DOT_ identifier_)?
    ;

dropBehaviour_
    : (CASCADE | RESTRICT)?
    ;