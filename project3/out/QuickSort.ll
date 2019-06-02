@.QuickSort_vtable = global [0 x i8*] []
@.QS_vtable = global [4 x i8*] [i8* bitcast (i32 (i8*,i32)* @QS.Start to i8*), i8* bitcast (i32 (i8*,i32,i32)* @QS.Sort to i8*), i8* bitcast (i32 (i8*)* @QS.Print to i8*), i8* bitcast (i32 (i8*,i32)* @QS.Init to i8*)]

declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
define void @print_int(i32 %i) {
    %_str = bitcast [4 x i8]* @_cint to i8*
    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
    ret void
}

define void @throw_oob() {
    %_str = bitcast [15 x i8]* @_cOOB to i8*
    call i32 (i8*, ...) @printf(i8* %_str)
    call void @exit(i32 1)
    ret void
}

define i32 @main() {
	%_0 = call i8* @calloc(i32 1, i32 20)
	%_1 = bitcast i8* %_0 to i8***
	%_2 = getelementptr [4 x i8*], [4 x i8*]* @.QS_vtable, i32 0, i32 0
	store i8** %_2, i8*** %_1
	%_3 = bitcast i8* %_0 to i8***
	%_4 = load i8**, i8*** %_3
	%_5 = getelementptr i8*, i8** %_4, i32 0
	%_6 = load i8*, i8** %_5
	%_7 = bitcast i8* %_6 to i32 (i8*, i32)*
	%_8 = call i32 %_7( i8* %_0, i32 10 )
	call void (i32) @print_int(i32 %_8)
	ret i32 0
}

define i32 @QS.Start(i8* %this, i32 %.sz){
	%sz = alloca i32
	store i32 %.sz, i32* %sz
	%aux01 = alloca i32
	%_0 = bitcast i8* %this to i8***
	%_1 = load i8**, i8*** %_0
	%_2 = getelementptr i8*, i8** %_1, i32 3
	%_3 = load i8*, i8** %_2
	%_4 = bitcast i8* %_3 to i32 (i8*, i32)*
	%_6 = load i32, i32* %sz
	%_5 = call i32 %_4( i8* %this, i32 %_6 )
	store i32 %_5, i32* %aux01
	%_7 = bitcast i8* %this to i8***
	%_8 = load i8**, i8*** %_7
	%_9 = getelementptr i8*, i8** %_8, i32 2
	%_10 = load i8*, i8** %_9
	%_11 = bitcast i8* %_10 to i32 (i8*)*
	%_12 = call i32 %_11( i8* %this )
	store i32 %_12, i32* %aux01
	call void (i32) @print_int(i32 9999)
	%_13 = getelementptr i8, i8* %this, i32 16
	%_14 = bitcast i8* %_13 to i32*
	%_15 = load i32, i32* %_14
	%_16 = sub i32 %_15, 1
	store i32 %_16, i32* %aux01
	%_17 = bitcast i8* %this to i8***
	%_18 = load i8**, i8*** %_17
	%_19 = getelementptr i8*, i8** %_18, i32 1
	%_20 = load i8*, i8** %_19
	%_21 = bitcast i8* %_20 to i32 (i8*, i32, i32)*
	%_23 = load i32, i32* %aux01
	%_22 = call i32 %_21( i8* %this, i32 0, i32 %_23 )
	store i32 %_22, i32* %aux01
	%_24 = bitcast i8* %this to i8***
	%_25 = load i8**, i8*** %_24
	%_26 = getelementptr i8*, i8** %_25, i32 2
	%_27 = load i8*, i8** %_26
	%_28 = bitcast i8* %_27 to i32 (i8*)*
	%_29 = call i32 %_28( i8* %this )
	store i32 %_29, i32* %aux01
	ret i32 0
}

define i32 @QS.Sort(i8* %this, i32 %.left, i32 %.right){
	%left = alloca i32
	store i32 %.left, i32* %left
	%right = alloca i32
	store i32 %.right, i32* %right
	%v = alloca i32
	%i = alloca i32
	%j = alloca i32
	%nt = alloca i32
	%t = alloca i32
	%cont01 = alloca i1
	%cont02 = alloca i1
	%aux03 = alloca i32
	store i32 0, i32* %t
	%_0 = load i32, i32* %left
	%_1 = load i32, i32* %right
	%_2 = icmp slt i32 %_0, %_1
	br i1 %_2, label %if0, label %if1
if0:
	%_3 = getelementptr i8, i8* %this, i32 8
	%_4 = bitcast i8* %_3 to i32**
	%_5 = load i32*, i32** %_4
	%_6 = load i32, i32* %right
	%_7 = load i32, i32* %_5
	%_8 = icmp ult i32 %_6, %_7
	br i1 %_8, label %oob0, label %oob1
oob0:
	%_9 = add i32 %_6, 1
	%_10 = getelementptr i32, i32* %_5, i32 %_9
	%_11 = load i32, i32* %_10
	br label %oob2
oob1:
	call void @throw_oob()
	br label %oob2
oob2:
	store i32 %_11, i32* %v
	%_12 = load i32, i32* %left
	%_13 = sub i32 %_12, 1
	store i32 %_13, i32* %i
	%_14 = load i32, i32* %right
	store i32 %_14, i32* %j
	store i1 1, i1* %cont01
	br label %loopstart0
loopstart0:
	%_15 = load i1, i1* %cont01
	br i1 %_15, label %next1, label %end2
next1:
	store i1 1, i1* %cont02
	br label %loopstart3
loopstart3:
	%_16 = load i1, i1* %cont02
	br i1 %_16, label %next4, label %end5
next4:
	%_17 = load i32, i32* %i
	%_18 = add i32 %_17, 1
	store i32 %_18, i32* %i
	%_19 = getelementptr i8, i8* %this, i32 8
	%_20 = bitcast i8* %_19 to i32**
	%_21 = load i32*, i32** %_20
	%_22 = load i32, i32* %i
	%_23 = load i32, i32* %_21
	%_24 = icmp ult i32 %_22, %_23
	br i1 %_24, label %oob3, label %oob4
oob3:
	%_25 = add i32 %_22, 1
	%_26 = getelementptr i32, i32* %_21, i32 %_25
	%_27 = load i32, i32* %_26
	br label %oob5
oob4:
	call void @throw_oob()
	br label %oob5
oob5:
	store i32 %_27, i32* %aux03
	%_28 = load i32, i32* %aux03
	%_29 = load i32, i32* %v
	%_30 = icmp slt i32 %_28, %_29
	%_31 = xor i1 1, %_30
	br i1 %_31, label %if3, label %if4
if3:
	store i1 0, i1* %cont02
	br label %if5
if4:
	store i1 1, i1* %cont02
	br label %if5
if5:
	br label %loopstart3
end5:
	store i1 1, i1* %cont02
	br label %loopstart6
loopstart6:
	%_32 = load i1, i1* %cont02
	br i1 %_32, label %next7, label %end8
next7:
	%_33 = load i32, i32* %j
	%_34 = sub i32 %_33, 1
	store i32 %_34, i32* %j
	%_35 = getelementptr i8, i8* %this, i32 8
	%_36 = bitcast i8* %_35 to i32**
	%_37 = load i32*, i32** %_36
	%_38 = load i32, i32* %j
	%_39 = load i32, i32* %_37
	%_40 = icmp ult i32 %_38, %_39
	br i1 %_40, label %oob6, label %oob7
oob6:
	%_41 = add i32 %_38, 1
	%_42 = getelementptr i32, i32* %_37, i32 %_41
	%_43 = load i32, i32* %_42
	br label %oob8
oob7:
	call void @throw_oob()
	br label %oob8
oob8:
	store i32 %_43, i32* %aux03
	%_44 = load i32, i32* %v
	%_45 = load i32, i32* %aux03
	%_46 = icmp slt i32 %_44, %_45
	%_47 = xor i1 1, %_46
	br i1 %_47, label %if6, label %if7
if6:
	store i1 0, i1* %cont02
	br label %if8
if7:
	store i1 1, i1* %cont02
	br label %if8
if8:
	br label %loopstart6
end8:
	%_48 = getelementptr i8, i8* %this, i32 8
	%_49 = bitcast i8* %_48 to i32**
	%_50 = load i32*, i32** %_49
	%_51 = load i32, i32* %i
	%_52 = load i32, i32* %_50
	%_53 = icmp ult i32 %_51, %_52
	br i1 %_53, label %oob9, label %oob10
oob9:
	%_54 = add i32 %_51, 1
	%_55 = getelementptr i32, i32* %_50, i32 %_54
	%_56 = load i32, i32* %_55
	br label %oob11
oob10:
	call void @throw_oob()
	br label %oob11
oob11:
	store i32 %_56, i32* %t
	%_57 = load i32, i32* %i
	%_58 = getelementptr i8, i8* %this, i32 8
	%_59 = bitcast i8* %_58 to i32**
	%_60 = load i32*, i32** %_59
	%_61 = load i32, i32* %j
	%_62 = load i32, i32* %_60
	%_63 = icmp ult i32 %_61, %_62
	br i1 %_63, label %oob12, label %oob13
oob12:
	%_64 = add i32 %_61, 1
	%_65 = getelementptr i32, i32* %_60, i32 %_64
	%_66 = load i32, i32* %_65
	br label %oob14
oob13:
	call void @throw_oob()
	br label %oob14
oob14:
	%_67 = getelementptr i8, i8* %this, i32 8
	%_68 = bitcast i8* %_67 to i32**
	%_69 = load i32*, i32** %_68
	%_70 = load i32, i32* %_69
	%_71 = icmp ult i32 %_57, %_70
	br i1 %_71, label %oob15, label %oob16

oob15:
	%_72 = add i32 %_57, 1
	%_73 = getelementptr i32, i32* %_69, i32 %_72
	store i32 %_66, i32* %_73
	br label %oob17
oob16:
	call void @throw_oob()
	br label %oob17
oob17:
	%_74 = load i32, i32* %j
	%_75 = load i32, i32* %t
	%_76 = getelementptr i8, i8* %this, i32 8
	%_77 = bitcast i8* %_76 to i32**
	%_78 = load i32*, i32** %_77
	%_79 = load i32, i32* %_78
	%_80 = icmp ult i32 %_74, %_79
	br i1 %_80, label %oob19, label %oob20

oob19:
	%_81 = add i32 %_74, 1
	%_82 = getelementptr i32, i32* %_78, i32 %_81
	store i32 %_75, i32* %_82
	br label %oob21
oob20:
	call void @throw_oob()
	br label %oob21
oob21:
	%_83 = load i32, i32* %j
	%_84 = load i32, i32* %i
	%_85 = add i32 %_84, 1
	%_86 = icmp slt i32 %_83, %_85
	br i1 %_86, label %if9, label %if10
if9:
	store i1 0, i1* %cont01
	br label %if11
if10:
	store i1 1, i1* %cont01
	br label %if11
if11:
	br label %loopstart0
end2:
	%_87 = load i32, i32* %j
	%_88 = getelementptr i8, i8* %this, i32 8
	%_89 = bitcast i8* %_88 to i32**
	%_90 = load i32*, i32** %_89
	%_91 = load i32, i32* %i
	%_92 = load i32, i32* %_90
	%_93 = icmp ult i32 %_91, %_92
	br i1 %_93, label %oob23, label %oob24
oob23:
	%_94 = add i32 %_91, 1
	%_95 = getelementptr i32, i32* %_90, i32 %_94
	%_96 = load i32, i32* %_95
	br label %oob25
oob24:
	call void @throw_oob()
	br label %oob25
oob25:
	%_97 = getelementptr i8, i8* %this, i32 8
	%_98 = bitcast i8* %_97 to i32**
	%_99 = load i32*, i32** %_98
	%_100 = load i32, i32* %_99
	%_101 = icmp ult i32 %_87, %_100
	br i1 %_101, label %oob26, label %oob27

oob26:
	%_102 = add i32 %_87, 1
	%_103 = getelementptr i32, i32* %_99, i32 %_102
	store i32 %_96, i32* %_103
	br label %oob28
oob27:
	call void @throw_oob()
	br label %oob28
oob28:
	%_104 = load i32, i32* %i
	%_105 = getelementptr i8, i8* %this, i32 8
	%_106 = bitcast i8* %_105 to i32**
	%_107 = load i32*, i32** %_106
	%_108 = load i32, i32* %right
	%_109 = load i32, i32* %_107
	%_110 = icmp ult i32 %_108, %_109
	br i1 %_110, label %oob30, label %oob31
oob30:
	%_111 = add i32 %_108, 1
	%_112 = getelementptr i32, i32* %_107, i32 %_111
	%_113 = load i32, i32* %_112
	br label %oob32
oob31:
	call void @throw_oob()
	br label %oob32
oob32:
	%_114 = getelementptr i8, i8* %this, i32 8
	%_115 = bitcast i8* %_114 to i32**
	%_116 = load i32*, i32** %_115
	%_117 = load i32, i32* %_116
	%_118 = icmp ult i32 %_104, %_117
	br i1 %_118, label %oob33, label %oob34

oob33:
	%_119 = add i32 %_104, 1
	%_120 = getelementptr i32, i32* %_116, i32 %_119
	store i32 %_113, i32* %_120
	br label %oob35
oob34:
	call void @throw_oob()
	br label %oob35
oob35:
	%_121 = load i32, i32* %right
	%_122 = load i32, i32* %t
	%_123 = getelementptr i8, i8* %this, i32 8
	%_124 = bitcast i8* %_123 to i32**
	%_125 = load i32*, i32** %_124
	%_126 = load i32, i32* %_125
	%_127 = icmp ult i32 %_121, %_126
	br i1 %_127, label %oob37, label %oob38

oob37:
	%_128 = add i32 %_121, 1
	%_129 = getelementptr i32, i32* %_125, i32 %_128
	store i32 %_122, i32* %_129
	br label %oob39
oob38:
	call void @throw_oob()
	br label %oob39
oob39:
	%_130 = bitcast i8* %this to i8***
	%_131 = load i8**, i8*** %_130
	%_132 = getelementptr i8*, i8** %_131, i32 1
	%_133 = load i8*, i8** %_132
	%_134 = bitcast i8* %_133 to i32 (i8*, i32, i32)*
	%_136 = load i32, i32* %left
	%_137 = load i32, i32* %i
	%_138 = sub i32 %_137, 1
	%_135 = call i32 %_134( i8* %this, i32 %_136, i32 %_138 )
	store i32 %_135, i32* %nt
	%_139 = bitcast i8* %this to i8***
	%_140 = load i8**, i8*** %_139
	%_141 = getelementptr i8*, i8** %_140, i32 1
	%_142 = load i8*, i8** %_141
	%_143 = bitcast i8* %_142 to i32 (i8*, i32, i32)*
	%_145 = load i32, i32* %i
	%_146 = add i32 %_145, 1
	%_147 = load i32, i32* %right
	%_144 = call i32 %_143( i8* %this, i32 %_146, i32 %_147 )
	store i32 %_144, i32* %nt
	br label %if2
if1:
	store i32 0, i32* %nt
	br label %if2
if2:
	ret i32 0
}

define i32 @QS.Print(i8* %this){
	%j = alloca i32
	store i32 0, i32* %j
	br label %loopstart9
loopstart9:
	%_0 = load i32, i32* %j
	%_1 = getelementptr i8, i8* %this, i32 16
	%_2 = bitcast i8* %_1 to i32*
	%_3 = load i32, i32* %_2
	%_4 = icmp slt i32 %_0, %_3
	br i1 %_4, label %next10, label %end11
next10:
	%_5 = getelementptr i8, i8* %this, i32 8
	%_6 = bitcast i8* %_5 to i32**
	%_7 = load i32*, i32** %_6
	%_8 = load i32, i32* %j
	%_9 = load i32, i32* %_7
	%_10 = icmp ult i32 %_8, %_9
	br i1 %_10, label %oob41, label %oob42
oob41:
	%_11 = add i32 %_8, 1
	%_12 = getelementptr i32, i32* %_7, i32 %_11
	%_13 = load i32, i32* %_12
	br label %oob43
oob42:
	call void @throw_oob()
	br label %oob43
oob43:
	call void (i32) @print_int(i32 %_13)
	%_14 = load i32, i32* %j
	%_15 = add i32 %_14, 1
	store i32 %_15, i32* %j
	br label %loopstart9
end11:
	ret i32 0
}

define i32 @QS.Init(i8* %this, i32 %.sz){
	%sz = alloca i32
	store i32 %.sz, i32* %sz
	%_0 = load i32, i32* %sz
	%_1 = getelementptr i8, i8* %this, i32 16
	%_2 = bitcast i8* %_1 to i32*
	store i32 %_0, i32* %_2
	%_3 = load i32, i32* %sz
	%_4 = icmp slt i32 %_3, 0
	br i1 %_4, label %arr_alloc0, label %arr_alloc1
arr_alloc0:
	call void @throw_oob()
	br label %arr_alloc1
arr_alloc1:
	%_5 = add i32 %_3, 1
	%_6 = call i8* @calloc(i32 4, i32 %_5)
	%_7 = bitcast i8* %_6 to i32*
	store i32 %_3, i32* %_7
	%_8 = getelementptr i8, i8* %this, i32 8
	%_9 = bitcast i8* %_8 to i32**
	store i32* %_7, i32** %_9
	%_10 = getelementptr i8, i8* %this, i32 8
	%_11 = bitcast i8* %_10 to i32**
	%_12 = load i32*, i32** %_11
	%_13 = load i32, i32* %_12
	%_14 = icmp ult i32 0, %_13
	br i1 %_14, label %oob44, label %oob45

oob44:
	%_15 = add i32 0, 1
	%_16 = getelementptr i32, i32* %_12, i32 %_15
	store i32 20, i32* %_16
	br label %oob46
oob45:
	call void @throw_oob()
	br label %oob46
oob46:
	%_17 = getelementptr i8, i8* %this, i32 8
	%_18 = bitcast i8* %_17 to i32**
	%_19 = load i32*, i32** %_18
	%_20 = load i32, i32* %_19
	%_21 = icmp ult i32 1, %_20
	br i1 %_21, label %oob48, label %oob49

oob48:
	%_22 = add i32 1, 1
	%_23 = getelementptr i32, i32* %_19, i32 %_22
	store i32 7, i32* %_23
	br label %oob50
oob49:
	call void @throw_oob()
	br label %oob50
oob50:
	%_24 = getelementptr i8, i8* %this, i32 8
	%_25 = bitcast i8* %_24 to i32**
	%_26 = load i32*, i32** %_25
	%_27 = load i32, i32* %_26
	%_28 = icmp ult i32 2, %_27
	br i1 %_28, label %oob52, label %oob53

oob52:
	%_29 = add i32 2, 1
	%_30 = getelementptr i32, i32* %_26, i32 %_29
	store i32 12, i32* %_30
	br label %oob54
oob53:
	call void @throw_oob()
	br label %oob54
oob54:
	%_31 = getelementptr i8, i8* %this, i32 8
	%_32 = bitcast i8* %_31 to i32**
	%_33 = load i32*, i32** %_32
	%_34 = load i32, i32* %_33
	%_35 = icmp ult i32 3, %_34
	br i1 %_35, label %oob56, label %oob57

oob56:
	%_36 = add i32 3, 1
	%_37 = getelementptr i32, i32* %_33, i32 %_36
	store i32 18, i32* %_37
	br label %oob58
oob57:
	call void @throw_oob()
	br label %oob58
oob58:
	%_38 = getelementptr i8, i8* %this, i32 8
	%_39 = bitcast i8* %_38 to i32**
	%_40 = load i32*, i32** %_39
	%_41 = load i32, i32* %_40
	%_42 = icmp ult i32 4, %_41
	br i1 %_42, label %oob60, label %oob61

oob60:
	%_43 = add i32 4, 1
	%_44 = getelementptr i32, i32* %_40, i32 %_43
	store i32 2, i32* %_44
	br label %oob62
oob61:
	call void @throw_oob()
	br label %oob62
oob62:
	%_45 = getelementptr i8, i8* %this, i32 8
	%_46 = bitcast i8* %_45 to i32**
	%_47 = load i32*, i32** %_46
	%_48 = load i32, i32* %_47
	%_49 = icmp ult i32 5, %_48
	br i1 %_49, label %oob64, label %oob65

oob64:
	%_50 = add i32 5, 1
	%_51 = getelementptr i32, i32* %_47, i32 %_50
	store i32 11, i32* %_51
	br label %oob66
oob65:
	call void @throw_oob()
	br label %oob66
oob66:
	%_52 = getelementptr i8, i8* %this, i32 8
	%_53 = bitcast i8* %_52 to i32**
	%_54 = load i32*, i32** %_53
	%_55 = load i32, i32* %_54
	%_56 = icmp ult i32 6, %_55
	br i1 %_56, label %oob68, label %oob69

oob68:
	%_57 = add i32 6, 1
	%_58 = getelementptr i32, i32* %_54, i32 %_57
	store i32 6, i32* %_58
	br label %oob70
oob69:
	call void @throw_oob()
	br label %oob70
oob70:
	%_59 = getelementptr i8, i8* %this, i32 8
	%_60 = bitcast i8* %_59 to i32**
	%_61 = load i32*, i32** %_60
	%_62 = load i32, i32* %_61
	%_63 = icmp ult i32 7, %_62
	br i1 %_63, label %oob72, label %oob73

oob72:
	%_64 = add i32 7, 1
	%_65 = getelementptr i32, i32* %_61, i32 %_64
	store i32 9, i32* %_65
	br label %oob74
oob73:
	call void @throw_oob()
	br label %oob74
oob74:
	%_66 = getelementptr i8, i8* %this, i32 8
	%_67 = bitcast i8* %_66 to i32**
	%_68 = load i32*, i32** %_67
	%_69 = load i32, i32* %_68
	%_70 = icmp ult i32 8, %_69
	br i1 %_70, label %oob76, label %oob77

oob76:
	%_71 = add i32 8, 1
	%_72 = getelementptr i32, i32* %_68, i32 %_71
	store i32 19, i32* %_72
	br label %oob78
oob77:
	call void @throw_oob()
	br label %oob78
oob78:
	%_73 = getelementptr i8, i8* %this, i32 8
	%_74 = bitcast i8* %_73 to i32**
	%_75 = load i32*, i32** %_74
	%_76 = load i32, i32* %_75
	%_77 = icmp ult i32 9, %_76
	br i1 %_77, label %oob80, label %oob81

oob80:
	%_78 = add i32 9, 1
	%_79 = getelementptr i32, i32* %_75, i32 %_78
	store i32 5, i32* %_79
	br label %oob82
oob81:
	call void @throw_oob()
	br label %oob82
oob82:
	ret i32 0
}

