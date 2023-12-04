package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.exceptions.*;
import java.util.HashMap;
import java.util.Stack;
public class SymbolTable {
    int currScope;
    int nextScope;
    Stack<Integer> scopeStack;

    public class Entry{

        NameDef namedef;
        int scope;
        Entry entryLink;
        Entry(NameDef nameDef, int scope, Entry entryLink){
            this.namedef = nameDef;
            this.scope = scope;
            this.entryLink = entryLink;
        }

    }
    HashMap<String, Entry> symbolTable;    //what is stored here?



    public SymbolTable(){
        currScope = 0;
        nextScope = currScope + 1;
        scopeStack = new Stack<Integer>();
        symbolTable = new HashMap<String, Entry>();
    }
    public void enterScope(){
        currScope = nextScope++;
        scopeStack.push(currScope);
    }
    public void exitScope(){
        currScope = scopeStack.pop();
    }
    public Entry lookup(String ident) throws TypeCheckException{
        //if scope of symbol is present in stack, proceed
        Entry entry = symbolTable.get(ident); //can be null
        //loop through entry chain until one w/ visible scope is found or reach end of chain (null)
        if(entry != null){
            while(!scopeStack.contains(entry.scope)){
                System.out.println("Entry lookup: " + entry.namedef.getName());
                entry = entry.entryLink;
                if(entry == null)
                    break;
            }
        }
        return entry;
    }
    public void insert(NameDef namedef) throws TypeCheckException {
        //build new entry
        String ident = namedef.getName();
        Entry entry = lookup(ident);

        if (entry != null && entry.scope == currScope) {    //handle duplicate variables
            throw new TypeCheckException(namedef.getName() + " is already defined in this scope");
        }

        setJavaName(entry, namedef);

        Entry entryInsert = new Entry(namedef, currScope, entry);
        symbolTable.put(ident, entryInsert);
    }
    public void setJavaName(Entry entry, NameDef namedef){
        if(entry == null){
            namedef.setJavaName(namedef.getName() + "$1");  //initial javaname
            return;
        }
        String javaName = entry.namedef.getJavaName();  //get javaname from previous entry
        int dollarIndex = javaName.indexOf("$");
        int javaNameIndex = Integer.parseInt(javaName.substring(dollarIndex + 1)) + 1; //PRECONDITION: javaName of previous entry is correctly formatted
        String newJavaName = namedef.getName() + "$" + javaNameIndex;
        namedef.setJavaName(newJavaName);
    }
}
