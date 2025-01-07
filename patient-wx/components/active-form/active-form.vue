<template>
    <view class="active-form form-container">
        <view class="form-box">
            <block v-for="(item, index) in formData" :key="item.id">
                <view class="form-item flex-row--c">
                    <!-- 单行文本框 -->
                    <view
                        class="line"
                        v-if="
              item.type === 'text' ||
              item.type === 'number' ||
              item.type === 'code'
            "
                    >
                        <view :class="item.rules.verify ? 'line-left' : 'p-l14 line-left'">
                            <text class="colorRed" v-if="item.rules.verify">*</text>
                            <text class="num" v-if="num">{{ index + 1 }}.</text>
                            {{ item.label }}
                        </view>
                        <!-- 发送验证码 -->
                        <view
                            class="line-right flex-row-sb-c pr20"
                            v-if="item.type === 'code'"
                        >
                            <input
                                type="text"
                                class="input"
                                :disabled="item.disabled"
                                placeholder-class="plaClass"
                                :placeholder="item.placeholder"
                                v-model="form[item.rules.name]"
                                @input="inputVal(index)"
                            />
                            <view style="width: 250rpx">
                                <u-button
                                    size="mini"
                                    type="primary"
                                    :disabled="item.disabled || isSend"
                                    @click="sendCode(item)"
                                >
                                    {{ codeFont }}
                                </u-button>
                            </view>
                        </view>
                        <!-- 普通输入框 -->
                        <view class="line-right" v-else>
                            <input
                                type="text"
                                class="input"
                                :disabled="item.disabled"
                                placeholder-class="plaClass"
                                :placeholder="item.placeholder"
                                v-model="form[item.rules.name]"
                                @input="inputVal(index)"
                            />
                        </view>
                    </view>
                    <!-- 下拉选择-->
                    <template v-else-if="item.type === 'select'">
                        <view class="line" @click="showSelect(item)">
                            <view
                                :class="item.rules.verify ? 'line-left' : 'p-l14 line-left'"
                            >
                                <text class="colorRed" v-if="item.rules.verify">*</text>
                                <text class="num" v-if="num">{{ index + 1 }}.</text>
                                {{ item.label }}
                            </view>
                            <view class="line-right pr20">
                                <view class="input plaClass">
                                    <text>
                                        {{
                                            (item.list.find(
                                                    (v) => v.value === form[item.rules.name]
                                                ) &&
                                                item.list.find((v) => v.value === form[item.rules.name])
                                                    .name) ||
                                            ""
                                        }}
                                    </text>
                                    <text class="input-placeholder" v-if="!form[item.rules.name]"
                                    >{{ item.placeholder }}
                                    </text>
                                </view>
                                <u-icon name="arrow-down-fill"></u-icon>
                            </view>
                        </view>
                        <u-action-sheet
                            :actions="item.list"
                            :title="item.title"
                            safeAreaInsetBottom
                            round="40"
                            cancelText="取消"
                            :show="item.show"
                            @select="selectConfirm($event, item)"
                            @close="selectClose(item)"
                        ></u-action-sheet>
                    </template>

                    <block v-else-if="item.type === 'datetime'">
                        <view class="line" @click="showSelect(item)">
                            <view
                                :class="item.rules.verify ? 'line-left' : 'p-l14 line-left'"
                            >
                                <text class="colorRed" v-if="item.rules.verify">*</text>
                                <text class="num" v-if="num">{{ index + 1 }}.</text>
                                {{ item.label }}
                            </view>
                            <view class="line-right pr20">
                                <view class="input plaClass">
                                    <text>{{ form[item.rules.name] }}</text>
                                    <text class="input-placeholder" v-if="!form[item.rules.name]"
                                    >{{ item.placeholder }}
                                    </text>
                                </view>
                                <u-icon name="arrow-down-fill"></u-icon>
                            </view>
                        </view>
                        <u-datetime-picker
                            :show="item.show"
                            :actions="item.list"
                            :title="item.title"
                            :mode="item.mode || 'date'"
                            :maxDate="
                item.maxDate ||
                new Date(new Date().getFullYear() + 10, 0, 1).getTime()
              "
                            :minDate="
                item.minDate ||
                new Date(new Date().getFullYear() - 10, 0, 1).getTime()
              "
                            :minHour="item.minHour || 0"
                            :maxHour="item.maxHour || 23"
                            :minMinute="item.minMinute || 0"
                            :maxMinute="item.maxMinute || 59"
                            :itemHeight="item.itemHeight || 44"
                            :formatter="item.formatter"
                            @confirm="selectDateTime($event, item)"
                            @close="selectClose(item)"
                            @cancel="selectClose(item)"
                        ></u-datetime-picker>
                    </block>

                    <template v-else-if="item.type === 'calendar'">
                        <view class="line" @click="showSelect(item)">
                            <view
                                :class="item.rules.verify ? 'line-left' : 'p-l14 line-left'"
                            >
                                <text class="colorRed" v-if="item.rules.verify">*</text>
                                <text class="num" v-if="num">{{ index + 1 }}.</text>
                                {{ item.label }}
                            </view>
                            <view class="line-right pr20">
                                <view class="input plaClass">
                                    <text>{{ form[item.rules.name] }}</text>
                                    <text class="input-placeholder" v-if="!form[item.rules.name]"
                                    >{{ item.placeholder }}
                                    </text>
                                </view>
                                <u-icon name="arrow-down-fill"></u-icon>
                            </view>
                        </view>
                        <u-calendar
                            :show="item.show"
                            :title="item.title"
                            :round="10"
                            :mode="item.mode || 'single'"
                            cancelText="取消"
                            @confirm="selectCalendar($event, item)"
                            @close="selectClose(item)"
                            :defaultDate="item.defaultDate"
                        ></u-calendar>
                    </template>

                    <!-- 多行文本框 -->
                    <view class="textarea-box" v-else-if="item.type === 'textarea'">
                        <view :class="item.rules.verify ? '' : 'p-l14 '">
                            <text class="colorRed" v-if="item.rules.verify">*</text>
                            <text class="num" v-if="num">{{ index + 1 }}.</text>
                            {{ item.label }}
                        </view>
                        <view class="line-bottom-textarea">
                            <!-- placeholder-class="plaClass" -->
                            <textarea
                                style="color: #a7a7a7; font-size: 24rpx"
                                auto-height
                                :maxlength="-1"
                                :disabled="item.disabled"
                                :placeholder="item.placeholder"
                                v-model="form[item.rules.name]"
                                @input="inputVal(index)"
                            >
              </textarea>
                        </view>
                    </view>
                    <!-- 上传图片 -->
                    <view
                        class="img-box pt30 flex-col-l"
                        v-else-if="item.type === 'file'"
                    >
                        <view class="font26" :class="item.rules.verify ? '' : 'p-l14 '">
                            <text class="colorRed" v-if="item.rules.verify">*</text>
                            <text class="num" v-if="num">{{ index + 1 }}.</text>
                            {{ item.label }}
                        </view>
                        <view class="img-upload p30">
                            <u-upload
                                :fileList="form[item.rules.name]"
                                :disabled="item.disabled"
                                :accept="item.accept"
                                :capture="item.capture"
                                :maxCount="item.maxCount"
                                :sizeType="item.sizeType"
                                :compressed="item.compressed"
                                :camera="item.camera"
                                :multiple="item.multiple"
                                :maxSize="item.maxSize"
                                :previewImage="item.previewImage"
                                width="150rpx"
                                height="150rpx"
                                @afterRead="afterRead($event, item)"
                                @delete="deletePic($event, item)"
                            ></u-upload>
                        </view>
                    </view>

                    <!-- 单选框 -->
                    <view class="line-col" v-else-if="item.type === 'radio'">
                        <view :class="item.rules.verify ? 'line-left' : 'p-l14 line-left'">
                            <text class="colorRed" v-if="item.rules.verify">*</text>
                            <text class="num" v-if="num">{{ index + 1 }}.</text>
                            {{ item.label }}
                            <text
                                style="
                  font-size: 20rpx;
                  color: #9e9e9e;
                  margin-left: 23rpx;
                  width: 140rpx;
                "
                            >(单选)
                            </text>
                        </view>
                        <view
                            class="line-bottom-select"
                            :style="{ 'padding-left': num ? '60rpx' : '34rpx' }"
                        >
                            <u-radio-group
                                v-model="form[item.rules.name]"
                                @change="radioChange($event, index)"
                                placement="row"
                            >
                                <u-radio
                                    :size="35"
                                    :icon-size="35"
                                    :label-size="25"
                                    shape="circle"
                                    v-for="(radioItem, radioIndex) in item.list"
                                    :key="radioIndex"
                                    :label="radioItem.label"
                                    :name="radioItem.value"
                                    :disabled="radioItem.disabled"
                                >
                                </u-radio>
                            </u-radio-group>
                        </view>
                    </view>
                    <!-- 多选框 -->
                    <view class="line-col" v-else-if="item.type === 'checkbox'">
                        <view :class="item.rules.verify ? 'line-left' : 'p-l14 line-left'">
                            <text class="colorRed" v-if="item.rules.verify">*</text>
                            <text class="num" v-if="num">{{ index + 1 }}.</text>
                            {{ item.label }}
                            <text
                                style="
                  font-size: 20rpx;
                  color: #9e9e9e;
                  margin-left: 23rpx;
                  width: 140rpx;
                "
                            >(多选)
                            </text>
                        </view>
                        <view
                            class="line-bottom-select pr20"
                            :style="{ 'padding-left': num ? '60rpx' : '34rpx' }"
                        >
                            <u-checkbox-group
                                v-model="form[item.rules.name]"
                                @change="checkboxGroupChange($event, item)"
                            >
                                <u-checkbox
                                    icon-size="25rpx"
                                    label-size="25rpx"
                                    size="30rpx"
                                    v-for="(checkboxItem, checkboxIndex) in item.list"
                                    :key="checkboxIndex"
                                    :label="checkboxItem.label"
                                    :disabled="checkboxItem.disabled"
                                    :name="checkboxItem.value"
                                >
                                </u-checkbox>
                            </u-checkbox-group>
                        </view>
                    </view>

                    <!-- 手机输入框 -->
                    <view class="line" v-else-if="item.type === 'mobile'">
                        <view :class="item.rules.verify ? 'line-left' : 'p-l14 line-left'">
                            <text class="colorRed" v-if="item.rules.verify">*</text>
                            <text class="num" v-if="num">{{ index + 1 }}.</text>
                            {{ item.label }}
                        </view>
                        <view class="line-right pr20">
                            <input
                                type="number"
                                v-model="form[item.rules.name]"
                                :placeholder="item.placeholder"
                                @input="inputVal(index)"
                                class="input"
                                :disabled="item.disabled"
                                :maxlength="11"
                                placeholder-class="plaClass"
                            />
                            <view style="width: 200rpx" v-if="item.oneKeyPhone">
                                <u-button
                                    size="mini"
                                    type="primary"
                                    open-type="getPhoneNumber"
                                    @getphonenumber="getphonenumber"
                                    :disabled="item.disabled"
                                >一键获取
                                </u-button>
                            </view>
                        </view>
                    </view>
                </view>
            </block>
        </view>
    </view>
</template>

<script>
import value from "../uview-ui/components/u-text/value";
import mixins from "./mixins";

export default {
    name: "activeForm",
    mixins: [mixins],
    props: {
        //是否展示序号
        num: {
            type: Boolean,
            default: false,
        },
        value: {
            type: Object,
            default: () => {
                return {};
            },
        },
        formData: {
            type: Array,
            default: () => {
                return [];
            },
        },
        //是否编辑表单
        isEdit: {
            type: Boolean,
            default: false,
        },
        //是否能删除
        isDel: {
            type: Boolean,
            default: false,
        },
    },
    data() {
        return {
            submitData: "",
            selectBox: [],
            currentSelectIndex: "",
            currentSelectValue: "",
            codeFont: "获取验证码",
            wait: 60,
            isSend: false,
            form: {}, //form//表单
        };
    },
    computed: {
        // formData: {
        //     get() {
        //         // console.log("this.formData", this.value)
        //         return this.value
        //     },
        //     set(nval) {
        //         this.$emit("input", nval);
        //     }
        // }
    },
    watch: {
        value: {
            handler(newVal, oldVal) {
                // console.log("newVal,oldVal", newVal, oldVal)
                this.dataFormat();
            },
            deep: true,
        }
    },
    mounted() {
        this.dataFormat();
    },
    methods: {
        //初始化表单
        dataFormat() {
            let formDefault = this.extractRules(this.formData);
            // console.log("formDefault :>> ", formDefault);
            let formValue = this.value;
            let form = Object.assign(formDefault, formValue);
            console.log("form", form);
            this.$set(this, "form", form);
        },
        //提取表单key和value
        extractRules(formData) {
            const extractedRules = {};

            formData.forEach((field) => {
                const {rules} = field;
                if (rules.name) {
                    extractedRules[rules.name] = rules.value;
                }
            });

            return extractedRules;
        },
        // 删除图片
        deletePic($event, item) {
            item.rules.value.splice($event.index, 1);
            this.form[item.rules.name] = item.rules.value;
            this.$emit("input", this.form);
        },
        // 新增图片
        afterRead($event, item) {
            // 当设置 mutiple 为 true 时, file 为数组格式，否则为对象格式
            item.rules.value = item.multiple ? $event.file : [$event.file];
            this.form[item.rules.name] = item.rules.value;
            this.$emit("input", this.form);
        },
        //显示select
        showSelect(item) {
            item.show = true;
            this.$emit("input", this.form);
            this.$forceUpdate();
        },
        //input输入框的值传给父组件
        inputVal(index) {
            const data = {
                val: this.formData[index].rules.value,
                index: index,
            };
            if (String(data.val) !== "" && String(data.index) !== "") {
                this.formData[data.index].rules.value = data.val;
            }
            this.$emit("input", this.form);
        },
        // 单选 下拉框点击确定
        selectConfirm($event, item) {
            item.rules.value = $event.value;
            this.form[item.rules.name] = $event.value;
            item.show = false;
            this.$emit("input", this.form);
            this.$forceUpdate();
        },
        selectCalendar($event, item) {
            const dateString = $event.reduce((b, n) => b + `,${n}`);
            console.log("selectCalendar", dateString);
            // item.rules.label = dateString;
            this.form[item.rules.name] = item.rules.value = dateString;
            item.show = false;
            this.$emit("input", this.form);
            this.$forceUpdate();
        },
        // value:返回所选时间戳，mode:当前模式
        selectDateTime($event, item) {
            const formatValue =
                $event.mode === "time"
                    ? this.formatTime(
                        this.getTimestampFromTime($event.value),
                        $event.mode
                    )
                    : this.formatTime($event.value, $event.mode);
            // item.rules.label = formatValue
            this.form[item.rules.name] = item.rules.value = item.returnTimestamp
                ? $event.mode === "time"
                    ? this.getTimestampFromTime($event.value)
                    : $event.value
                : formatValue; //是否返回时间搓
            item.show = false;
            this.$emit("input", this.form);
            this.$forceUpdate();
        },
        // 帮我更具 参数	date为日期选择，time为时间选择，year-month为年月选择 ,datetime日期时间选择 分别把时间搓转换成对应的格式
        formatTime(timestamp, formatType) {
            const date = new Date(timestamp);

            if (isNaN(date)) {
                return "Invalid Date";
            }

            let year = date.getFullYear();
            let month = String(date.getMonth() + 1).padStart(2, "0");
            let day = String(date.getDate()).padStart(2, "0");
            let hours = String(date.getHours()).padStart(2, "0");
            let minutes = String(date.getMinutes()).padStart(2, "0");
            let seconds = String(date.getSeconds()).padStart(2, "0");

            switch (formatType) {
                case "date":
                    return `${year}-${month}-${day}`;
                case "time":
                    return `${hours}:${minutes}`; //:${seconds}
                case "year-month":
                    return `${year}-${month}`;
                case "datetime":
                    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
                default:
                    return "Invalid Format Type";
            }
        },
        // 根据给定的时间（"11:00"）获取当前日期和指定时间的时间戳
        getTimestampFromTime(inputTime) {
            // 获取当前日期
            const currentDate = new Date();

            // 将输入时间字符串拆分成小时和分钟
            const [hours, minutes] = inputTime.split(":");

            // 创建一个新日期对象，设置时间为输入的小时和分钟
            const targetDate = new Date(
                currentDate.getFullYear(),
                currentDate.getMonth(),
                currentDate.getDate(),
                hours,
                minutes
            );

            // 获取时间戳
            const timestamp = targetDate.getTime();

            return timestamp;
        },
        selectClose(item) {
            item.show = false;
            this.$forceUpdate();
        },
        //单选 点击触发
        radioChange($event, index) {
            this.$emit("input", this.form);
        },
        //复选框 点击触发
        checkboxGroupChange($event, item) {
            // console.log("$event", $event)
            const selectArr = item.list.filter((v) => $event.includes(v.label)); //过滤
            // console.log("selectArr", selectArr)
            this.form[item.rules.name] = item.rules.value = selectArr.map(
                (v) => v.value
            );
            this.$emit("input", this.form);
        },

        // 发送验证码
        sendCode(item) {
            let setTime = 0;
            this.sendCodeCallback(item);
            if (!this.isSend) {
                this.isSend = true;
                setTime = setInterval(() => {
                    this.wait--;

                    this.codeFont = this.wait + "重新发送";
                    if (this.wait === 0) {
                        clearInterval(setTime);
                        this.codeFont = "获取验证码";
                        this.isSend = false;
                        this.wait = 60;
                    }
                }, 1000);
            }
        },
        //校验
        vervify() {
            return new Promise((resolve, reject) => {
                this.formData.forEach((item) => {
                    if (item.rules.verify) {
                        switch (item.type) {
                            case "checkbox":
                                if (this.form[item.rules.name].length === 0) {
                                    uni.showToast({
                                        title: item.rules.errMess || "请选择" + item.label,
                                        duration: 2000,
                                        icon: "none",
                                    });

                                    throw Error(); //终止函数
                                }
                                break;
                            case "file":
                                if (this.form[item.rules.name].length === 0) {
                                    uni.showToast({
                                        title: item.rules.errMess || "请选择" + item.label,
                                        duration: 2000,
                                        icon: "none",
                                    });

                                    throw Error(); //终止函数
                                }
                                break;
                            case "mobile":
                                if (!this.form[item.rules.name]) {
                                    uni.showToast({
                                        title: "手机号不能为空",
                                        duration: 2000,
                                        icon: "none",
                                    });

                                    throw Error(); //终止函数
                                }
                                if (!/^\s{0}$|^1\d{10}$/.test(this.form[item.rules.name])) {
                                    uni.showToast({
                                        title: "手机格式错误",
                                        duration: 2000,
                                        icon: "none",
                                    });

                                    throw Error(); //终止函数
                                }
                                break;
                            default:
                                if (!this.form[item.rules.name]) {
                                    uni.showToast({
                                        title: item.rules.errMess || item.label + "不能为空",
                                        duration: 2000,
                                        icon: "none",
                                    });

                                    throw Error(); //终止函数
                                }
                                if (
                                    item.rules.regexp &&
                                    !new RegExp(item.rules.regexp).test(
                                        this.form[item.rules.name]
                                    )
                                ) {
                                    uni.showToast({
                                        title: item.label + "格式不正确",
                                        duration: 2000,
                                        icon: "none",
                                    });

                                    throw Error(); //终止函数
                                }
                                break;
                        }
                    }
                });
                resolve(this.form);
            });
        },
        //重置表单
        resetForm() {
            for (let key in this.form) {
                if (this.form.hasOwnProperty(key)) {
                    switch (typeof this.form[key]) {
                        case "object":
                            this.form[key] = [];
                            break;
                        case "number":
                            this.form[key] = "";
                            break;
                        case "string":
                            this.form[key] = "";
                            break;
                    }
                }
            }
            this.$emit("input", this.form);
        },
        // 提交序列化的表单
        // $submitForm() {
        //     const formData = this.formData
        //     console.log("formData :>> ", formData);
        //     let submitData = {};
        //     for (let i = 0; i < formData.length; i++) {
        //         if (formData[i].type === 'file') {
        //             submitData[formData[i].rules.name] = formData[i].rules.fileList;
        //             continue;
        //         }
        //         submitData[formData[i].rules.name] = formData[i].rules.value;
        //     }
        //     return submitData;
        // }
    },
};
</script>

<style lang="scss" scoped>
.active-form {
    min-height: 200px;

    .plaClass {
        text-align: right;
        font-size: 26rpx;
    }

    .input-placeholder {
        display: flex;
        justify-content: flex-end;
        font-size: 26rpx;
        color: grey;
    }

    // 下拉icon
    .select-icon {
        float: right;
        border-bottom: 2rpx solid #999;
        border-right: 2rpx solid #999;
        width: 16rpx;
        height: 16rpx;
        transform: rotate(-45deg);
        margin-right: 10rpx;
    }

    .form-box {
        width: 100%;
        box-sizing: border-box;
    }

    .colorRed {
        color: red;
        padding: 0 10rpx;
    }

    .line-right {
        flex: 1;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: space-between;
        position: relative;
    }

    .line-bottom-select {
        padding-top: 40rpx;
    }

    .line-bottom-textarea {
        padding-top: 20rpx;

        textarea {
            min-height: 100rpx;
        }
    }

    .line-left {
        display: flex;
        min-width: 22%;
        align-items: center;
        height: 100%;
        letter-spacing: 0.5px;
        font-size: 26rpx;
        color: #000000;
        box-sizing: border-box;
    }

    .p-l14 {
        padding-left: 34rpx;
    }

    .textarea-box {
        width: 100%;
        border-bottom: 1px solid #ededed;
        padding-top: 20rpx;
        font-size: 26rpx;

        textarea {
            padding-left: 32rpx;
            height: 100rpx;
            font-size: 25rpx;
            color: #333;
        }
    }

    .line {
        padding: 27rpx 0;
        display: flex;
        align-items: center;
        width: 100%;
        margin: 0 auto;
        border-bottom: 1px solid #ededed;
        overflow: hidden;

        .input {
            padding-right: 20rpx;
            padding-left: 20rpx;
            height: 100%;
            width: 100%;
            text-align: left;
            font-size: 28rpx;
            color: #333;
            border: none;
            overflow: hidden;
            text-overflow: ellipsis;
            outline: none;
        }
    }

    .line-col {
        padding: 27rpx 0;
        display: flex;
        flex-direction: column;
        width: 100%;
        margin: 0 auto;
        border-bottom: 1px solid #ededed;
        overflow: hidden;

        .input {
            padding-right: 20rpx;
            height: 100%;
            width: 100%;
            text-align: left;
            font-size: 28rpx;
            color: #333;
            border: none;
            overflow: hidden;
            text-overflow: ellipsis;
            outline: none;
        }
    }

    .num {
        margin-right: 8rpx;
    }
}
</style>
