# 导入即用 全端支持
### 2.0插件地址  https://ext.dcloud.net.cn/plugin?id=5842  （基于uview2.0）
### 1.0插件地址  https://ext.dcloud.net.cn/plugin?id=8755  （基于uview1.0）

#### 注意！！！
``` 
本插件基于 uview2.0进行重构 使用需要引入uview2.0支持
[ 点击uview2.0官网地址 ](https://www.uviewui.com/)
[引入uview2.0指南](https://ext.dcloud.net.cn/plugin?id=1593)
```
### 有问题 可评论 看到及时回复
# 使用方式
## 1. 注册
### App.vue引入组件样式即可
```javascript
<script>
	export default {
	onLaunch: function() {
	console.log('App Launch')
},
	onShow: function() {
	console.log('App Show')
},
	onHide: function() {
	console.log('App Hide')
}
}
</script>

<style lang="scss">
	/*每个页面公共css */
	@import "@/components/active-form/active-form.scss";//注入active-form样式
</style>
```


## 2.页面使用

```javascript
<template>
    <view class="content">
        <active-form ref="activeForm" v-model="form" :formData="formData" num></active-form>

        <view class="btn-list">
            <view class="subform" @click="submit">提交表单</view>
            <view class="resetform" @click="reset">重置表单</view>
        </view>
    </view>
</template>

<script>
import ActiveForm from "@/components/active-form/active-form";

export default {
    components: {
        ActiveForm,
    },
    data() {
        return {
            //双向绑定回显
            form: {
                age: 2,
                // createTime:"00:03"
                // photo: [{url: 'https://uviewui.com/common/logo.png'}],
                // sex:1
            },
            //表单配置项
            formData: [
                {
                    id: "sad31asgh",
                    placeholder: "请选择年龄段",
                    label: "年龄",
                    type: "radio",
                    list: [
                        {
                            value: 1,
                            label: "15岁以下",
                        },
                        {
                            value: 2,
                            label: "16~20岁",
                        },
                        {
                            value: 3,
                            label: "21~25岁",
                        },
                        {
                            value: 4,
                            label: "26~30岁",
                        },
                        {
                            value: 5,
                            label: "31~40岁",
                        },
                        {
                            value: 6,
                            label: "40岁以上",
                        },
                    ],
                    rules: {
                        name: "age",
                        value: 1, // 字段值  list.value 填入回显 0 1
                        verify: true,
                        errMess: "请选择年龄段",
                    },
                },
                {
                    id: "kjj32nggg22112sas2asd",
                    placeholder: "未选择获客时间",
                    label: "获客时间",
                    type: "datetime",
                    mode: 'datetime',//	date为日期选择，time为时间选择，year-month为年月选择 ,datetime日期时间选择
                    returnTimestamp: false, //value值是否返回时间搓格式
                    // maxDate: new Date(new Date().getFullYear() + 10, 0, 1).getTime(),//可选的最大时间（时间戳毫秒）
                    // minDate: new Date(new Date().getFullYear() - 10, 0, 1).getTime(),//可选的最小时间（时间戳毫秒）
                    // minHour: 0,//可选的最小小时，仅mode=time有效
                    // maxHour: 23,//可选的最大小时，仅mode=time有效
                    // minMinute: 0,//可选的最小分钟，仅mode=time有效
                    // maxMinute: 59,//可选的最大分钟，仅mode=time有效
                    show: false,
                    rules: {
                        name: "createTime",
                        value: "",
                        verify: false,
                        errMess: "未选择获客时间",
                    },
                },
                {
                    id: "kjj32nsa123s232asd",
                    placeholder: "选择日期",
                    label: "日期",
                    type: "calendar",
                    mode: 'multiple',//	mode为single只能选择单个日期 为multiple可以选择多个日期为range可以选择日期范围
                    returnTimestamp: false, //value值是否返回时间搓格式
                    show: false,
                    rules: {
                        name: "calendarDate",
                        value: "",
                        verify: false,
                        errMess: "未选择获客时间",
                    },
                },
                {
                    id: "kjjn123sasd",
                    placeholder: "输入名字",
                    label: "姓名",
                    type: "text",
                    disabled: false,
                    rules: {
                        name: "name",
                        value: "",
                        verify: true,
                        errMess: "姓名未填写",
                        regexp: '^1'  //正则校验
                    },
                },
                {
                    id: "kjjnsas123d",
                    placeholder: "输入验证码",
                    label: "验证码",
                    type: "code",
                    disabled: false,//是否禁用
                    rules: {
                        name: "code",
                        value: "",
                        verify: true,
                        errMess: "验证码未填写",
                    },
                    // 发送验证码回调   由于小程序props传递函数会丢失（此处无效） 此回调方法在组建内部书写  方法在目录active-form/mixins.js中自定义书写
                    // sendCodeCallback() {
                    //
                    // }
                },
                {
                    id: "uisd123fjks",
                    placeholder: "请输入手机号",
                    label: "手机",
                    type: "mobile",
                    rules: {
                        name: "mobile",
                        value: "", //字段值
                        verify: true,
                        errMess: "手机号格式不正确",
                    },
                    oneKeyPhone: true, //是否开启微信点击获取手机号
                    //微信获取手机号回调  和参数oneKeyPhone配套  由于小程序props传递函数会丢失（此处无效） 方法在目录active-form/mixins.js中自定义书写
                    // getphonenumber({details}) {
                    //
                    // }
                },
                {
                    id: "ukhjgas12dsas",
                    placeholder: "选择",
                    label: "性别",
                    type: "select",
                    show: false, //是否显示
                    title: "请选择性别",
                    list: [
                        {
                            value: 1,
                            name: "男",
                        },
                        {
                            value: 2,
                            name: "女",
                        },
                    ],
                    rules: {
                        name: "sex",
                        value: "", //list.value  0 1
                        verify: false,
                        errMess: "性别未选择",
                    },
                },
                {
                    id: "gdfjZjnx",
                    placeholder: "请输入地址信息",
                    label: "地址 ",
                    type: "text",
                    rules: {
                        name: "location",
                        value: "",
                        verify: false,
                        errMess: "地址不能为空",
                    },
                },
                {
                    id: "asdfd11fgd",
                    label: "孩子头像 ",
                    type: "file",
                    maxCount: 1, //最大选择图片的数量
                    multiple: false, //是否开启图片多选，部分安卓机型不支持  false	true
                    accept: 'image',//接受的文件类型，file只支持H5（只有微信小程序才支持把accept配置为all、media）  可选 all | media | image | file | video
                    capture: ['album', 'camera'],// 图片或视频拾取模式，当accept为image类型时设置capture可选额外camera可以直接调起摄像头 String | Array
                    sizeType: ['original', 'compressed'],//original 原图，compressed 压缩图，默认二者都有，H5无效
                    compressed: true,//当accept为video时生效，是否压缩视频，默认为true  Boolean	true	false
                    camera: 'back', //当accept为video时生效，可选值为back或front  	String	back	-
                    maxSize: Number.MAX_VALUE,// 选择单个文件的最大大小，单位B(byte)，默认不限制	String | Number
                    previewImage: true,//是否在上传完成后展示预览图	Boolean	true	false
                    disabled: false,//是否禁用
                    rules: {
                        name: "photo",
                        value: [],//显示已上传的文件列表  回显   [{url:'https://xxx.cn'}]
                        verify: true,
                        errMess: "请选择头像",
                    },
                },
                {
                    id: "gdoodsndf",
                    placeholder: "请选择兴趣",
                    label: "兴趣",
                    type: "checkbox",
                    list: [
                        {
                            value: 1,
                            label: "篮球",
                            disabled: true,//是否禁用
                        },
                        {
                            value: 2,
                            label: "足球",
                        },
                        {
                            value: 3,
                            label: "羽毛球",
                        },
                        {
                            value: 4,
                            label: "唱歌",
                        },
                        {
                            value: 5,
                            label: "街舞",
                        },
                        {
                            value: 6,
                            label: "yyds",
                        },
                        {
                            value: 7,
                            label: "跳舞",
                        },
                        {
                            value: 8,
                            label: "看剧",
                        },
                        {
                            value: 9,
                            label: "洗澡",
                        },
                    ],
                    rules: {
                        name: "interest",
                        value: [7], //字段值 0 1
                        verify: false,
                        errMess: "请选择年龄段",
                    },
                },
                {
                    id: "kopty53mk",
                    placeholder: "请输入公司名称",
                    label: "公司",
                    type: "text",
                    rules: {
                        name: "company",
                        value: "",
                        verify: false,
                        errMess: "公司名称不能为空",
                    },
                },
                {
                    id: "asd3453fgf",
                    placeholder: "(例如：平面设计、品牌设计、UI设计)",
                    label: "是否有设计相关经历？",
                    type: "textarea",
                    rules: {
                        name: "experience",
                        value: "", //字段值
                        verify: true,
                        errMess: "请输入经历",
                    },
                },
            ],
        };
    },
    methods: {
        // 提交表单
        submit() {
            this.$refs.activeForm.vervify()
                .then(async (form) => {
                    console.log("表单对象:", form);
                })
                .catch((err) => {
                    console.log("err", err);
                });  //表单校验
        },
        //重置表单
        reset() {
            this.$refs.activeForm.resetForm()
        }
    },
};
</script>

<style lang="less">
.content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .btn-list {
        display: flex;
        .subform {
            margin: 30rpx;
            padding: 20rpx 60rpx;
            border-radius: 18rpx;
            background-color: bisque;
        }

        .resetform {
            margin: 30rpx;
            padding: 20rpx 60rpx;
            border-radius: 18rpx;
            background-color: #6e6a67;
            color: #ffffff;
        }
    }

}
</style>


```


# 组件参数


### 1.组件props

| 参数名           | 类型            | 是否必填 | 介绍                                                                            |
| ------------- | ------------- | ---- | -----------------------------------------------------------------------------|
| form  | Object       | 是    | v-model双向绑定（若要回显在此处定义，表单值）    |
| formData  | Array       | 是    | 表单配置项option
| num  | Boolean 默认false       | 是   | 每个表单项前面是否带有序号

### 2.formData 项参数说明 (type=datetime、calendar ,部分参数参考uview2.0官方文档)

| 参数名           | 类型            | 是否必填 | 介绍                                                                            |
| ------------- | ------------- | ---- | ----------------------------------------------------------------------------- |
| id            | String number | 是    | 必填索引 不唯一                                                                         |
| placeholder   | String        | 是    | 提示
| label   | String        | 是    | 开头标题         |
| type          | String        | 是    | 该表单项类型 当前支持 text(文本)、 number、 mobile、code(验证码)、 radio、 checkbox、 file(照片,文件)、 select(上拉选择)、calendar(日期选择) 、 datetime(时间选择)|
| list          | Array         | 是    | type为 radio、 checkbox、select时有效
| list[index].disabled         | Boolean         | 否    | type为 radio、 checkbox、select时有效
| disabled          | Boolean         | 否    | type为 radio、 checkbox、select时有效
| rules.errMess | String        | 是    | 校验不通过时的错误提示                                                                   |
| rules.name    | String        | 是    | 接收字段名 后端接收的字段                                                                 |
| rules.value   | String Array      | 否    | 默认值(一开始显示),回显使用form对象                                                                           |
| rules.verify  | Boolean       | 否    | 是否校验该字段
| rules.regexp  | String       | 否   | 正侧校验字段

### 3.校验方法

| 事件名                         | 返回参数   | 简介                     |
| --------------------------- | ------ | ---------------------- |
| vervify()     | null   | *表单校验 成功会继续往下走 失败抛出异常* |
| resetForm(); |  null | *重置表单*           |

```javascript
##记得组件绑定ref
methods: {
	// 提交表单
	submit() {
		this.$refs.activeForm.vervify()
			.then(async (form) => {
				console.log("表单对象:", form);
			})
			.catch((err) => {
				console.log("err", err);
			});  //表单校验
	},
	//重置表单
	reset() {
		this.$refs.activeForm.resetForm()
	}
},
```
```


### 4.由于小程序props传递函数会丢失（此处无效） 选项回调方法在目录active-form/mixins.js中自定义书写 
```javascript
export default {
    data() {
        return {}
    },
    methods: {
        // 微信小程序获取手机号
        async getphonenumber({detail}) {

        },
        // 获取验证码回调 此处调用api接口 并赋值
        async sendCodeCallback(item) {
            // item.rules.value =
        }
    }
}

```
