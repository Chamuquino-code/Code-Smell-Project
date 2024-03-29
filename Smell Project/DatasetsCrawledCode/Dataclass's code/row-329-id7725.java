@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)


public class SortableASTTransformation extends AbstractASTTransformation {
 private static final ClassNode MY_TYPE = make(Sortable.class);
 private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
 private static final ClassNode COMPARABLE_TYPE = makeClassSafe(Comparable.class);
 private static final ClassNode COMPARATOR_TYPE = makeClassSafe(Comparator.class);


 private static final String VALUE = "value";
 private static final String OTHER = "other";
 private static final String THIS_HASH = "thisHash";
 private static final String OTHER_HASH = "otherHash";
 private static final String ARG0 = "arg0";
 private static final String ARG1 = "arg1";


 public void visit(ASTNode[] nodes, SourceUnit source) {
 init(nodes, source);
 AnnotationNode annotation = (AnnotationNode) nodes[0];
 AnnotatedNode parent = (AnnotatedNode) nodes[1];
 if (parent instanceof ClassNode) {
 createSortable(annotation, (ClassNode) parent);
        }
    }


 private void createSortable(AnnotationNode anno, ClassNode classNode) {
 List<String> includes = getMemberStringList(anno, "includes");
 List<String> excludes = getMemberStringList(anno, "excludes");
 boolean reversed = memberHasValue(anno, "reversed", true);
 boolean includeSuperProperties = memberHasValue(anno, "includeSuperProperties", true);
 boolean allNames = memberHasValue(anno, "allNames", true);
 boolean allProperties = !memberHasValue(anno, "allProperties", false);
 if (!checkIncludeExcludeUndefinedAware(anno, excludes, includes, MY_TYPE_NAME)) return;
 if (!checkPropertyList(classNode, includes, "includes", anno, MY_TYPE_NAME, false, includeSuperProperties, allProperties)) return;
 if (!checkPropertyList(classNode, excludes, "excludes", anno, MY_TYPE_NAME, false, includeSuperProperties, allProperties)) return;
 if (classNode.isInterface()) {
 addError(MY_TYPE_NAME + " cannot be applied to interface " + classNode.getName(), anno);
        }
 List<PropertyNode> properties = findProperties(anno, classNode, includes, excludes, allProperties, includeSuperProperties, allNames);
 implementComparable(classNode);


 addGeneratedMethod(classNode,
 "compareTo",
 ACC_PUBLIC,
 ClassHelper.int_TYPE,
 params(param(newClass(classNode), OTHER)),
 ClassNode.EMPTY_ARRAY,
 createCompareToMethodBody(properties, reversed)
        );


 for (PropertyNode property : properties) {
 createComparatorFor(classNode, property, reversed);
        }
 new VariableScopeVisitor(sourceUnit, true).visitClass(classNode);
    }


 private static void implementComparable(ClassNode classNode) {
 if (!classNode.implementsInterface(COMPARABLE_TYPE)) {
 classNode.addInterface(makeClassSafeWithGenerics(Comparable.class, classNode));
        }
    }


 private static Statement createCompareToMethodBody(List<PropertyNode> properties, boolean reversed) {
 List<Statement> statements = new ArrayList<Statement>();


 // if (this.is(other)) return 0;
 statements.add(ifS(callThisX("is", args(OTHER)), returnS(constX(0))));


 if (properties.isEmpty()) {
 // perhaps overkill but let compareTo be based on hashes for commutativity
 // return this.hashCode() <=> other.hashCode()
 statements.add(declS(localVarX(THIS_HASH, ClassHelper.Integer_TYPE), callX(varX("this"), "hashCode")));
 statements.add(declS(localVarX(OTHER_HASH, ClassHelper.Integer_TYPE), callX(varX(OTHER), "hashCode")));
 statements.add(returnS(compareExpr(varX(THIS_HASH), varX(OTHER_HASH), reversed)));
        } else {
 // int value = 0;
 statements.add(declS(localVarX(VALUE, ClassHelper.int_TYPE), constX(0)));
 for (PropertyNode property : properties) {
 String propName = property.getName();
 // value = this.prop <=> other.prop;
 statements.add(assignS(varX(VALUE), compareExpr(propX(varX("this"), propName), propX(varX(OTHER), propName), reversed)));
 // if (value != 0) return value;
 statements.add(ifS(neX(varX(VALUE), constX(0)), returnS(varX(VALUE))));
            }
 // objects are equal
 statements.add(returnS(constX(0)));
        }


 final BlockStatement body = new BlockStatement();
 body.addStatements(statements);
 return body;
    }


 private static Statement createCompareMethodBody(PropertyNode property, boolean reversed) {
 String propName = property.getName();
 return block(
 // if (arg0 == arg1) return 0;
 ifS(eqX(varX(ARG0), varX(ARG1)), returnS(constX(0))),
 // if (arg0 != null && arg1 == null) return -1;
 ifS(andX(notNullX(varX(ARG0)), equalsNullX(varX(ARG1))), returnS(constX(-1))),
 // if (arg0 == null && arg1 != null) return 1;
 ifS(andX(equalsNullX(varX(ARG0)), notNullX(varX(ARG1))), returnS(constX(1))),
 // return arg0.prop <=> arg1.prop;
 returnS(compareExpr(propX(varX(ARG0), propName), propX(varX(ARG1), propName), reversed))
        );
    }


 private static void createComparatorFor(ClassNode classNode, PropertyNode property, boolean reversed) {
 String propName = StringGroovyMethods.capitalize((CharSequence) property.getName());
 String className = classNode.getName() + "$" + propName + "Comparator";
 ClassNode superClass = makeClassSafeWithGenerics(AbstractComparator.class, classNode);
 InnerClassNode cmpClass = new InnerClassNode(classNode, className, ACC_PRIVATE | ACC_STATIC, superClass);
 addGeneratedInnerClass(classNode, cmpClass);


 addGeneratedMethod(cmpClass,
 "compare",
 ACC_PUBLIC,
 ClassHelper.int_TYPE,
 params(param(newClass(classNode), ARG0), param(newClass(classNode), ARG1)),
 ClassNode.EMPTY_ARRAY,
 createCompareMethodBody(property, reversed)
        );


 String fieldName = "this$" + propName + "Comparator";
 // private final Comparator this$<property>Comparator = new <type>$<property>Comparator();
 FieldNode cmpField = classNode.addField(
 fieldName,
 ACC_STATIC | ACC_FINAL | ACC_PRIVATE | ACC_SYNTHETIC,
 COMPARATOR_TYPE,
 ctorX(cmpClass));


 addGeneratedMethod(classNode,
 "comparatorBy" + propName,
 ACC_PUBLIC | ACC_STATIC,
 COMPARATOR_TYPE,
 Parameter.EMPTY_ARRAY,
 ClassNode.EMPTY_ARRAY,
 returnS(fieldX(cmpField))
        );
    }


 private List<PropertyNode> findProperties(AnnotationNode annotation, final ClassNode classNode, final List<String> includes,
 final List<String> excludes, final boolean allProperties,
 final boolean includeSuperProperties, final boolean allNames) {
 Set<String> names = new HashSet<String>();
 List<PropertyNode> props = getAllProperties(names, classNode, classNode, true, false, allProperties,
 false, includeSuperProperties, false, false, allNames, false);
 List<PropertyNode> properties = new ArrayList<PropertyNode>();
 for (PropertyNode property : props) {
 String propertyName = property.getName();
 if ((excludes != null && excludes.contains(propertyName)) ||
 includes != null && !includes.contains(propertyName)) continue;
 properties.add(property);
        }
 for (PropertyNode pNode : properties) {
 checkComparable(pNode);
        }
 if (includes != null) {
 Comparator<PropertyNode> includeComparator = new Comparator<PropertyNode>() {
 public int compare(PropertyNode o1, PropertyNode o2) {
 return Integer.compare(includes.indexOf(o1.getName()), includes.indexOf(o2.getName()));
                }
            };
 Collections.sort(properties, includeComparator);
        }
 return properties;
    }


 private void checkComparable(PropertyNode pNode) {
 if (pNode.getType().implementsInterface(COMPARABLE_TYPE) || isPrimitiveType(pNode.getType()) || hasAnnotation(pNode.getType(), MY_TYPE)) {
 return;
        }
 addError("Error during " + MY_TYPE_NAME + " processing: property '" +
 pNode.getName() + "' must be Comparable", pNode);
    }


 /**
     * Helper method used to build a binary expression that compares two values
     * with the option to handle reverse order.
     */
 private static BinaryExpression compareExpr(Expression lhv, Expression rhv, boolean reversed) {
 return (reversed) ? cmpX(rhv, lhv) : cmpX(lhv, rhv);
    }


}